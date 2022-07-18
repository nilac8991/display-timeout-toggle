package com.zebra.nilac.displaytimeouttoggle.utils

import android.content.Context
import android.util.Log
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.EMDKResults.STATUS_CODE
import com.symbol.emdk.ProfileManager
import com.symbol.emdk.ProfileManager.PROFILE_FLAG
import kotlinx.coroutines.*

class EMDKLoader {

    private var mEmdkManager: EMDKManager? = null
    private var mProfileResultCallBacks: EMDKProfileResultCallBacks? = null
    private var mEMDKManagerInitCallBacks: EMDKManagerInitCallBacks? = null

    private var initScope = MainScope()
    private var profileProcessScope = MainScope()

    fun initEMDKManager(context: Context) {
        initEMDKManager(context, null)
    }

    fun initEMDKManager(context: Context, emdkManagerInitCallBacks: EMDKManagerInitCallBacks?) {
        Log.i(TAG, "Initialising EMDK Manager asynchronously")
        mEMDKManagerInitCallBacks = emdkManagerInitCallBacks

        initScope.launch(Dispatchers.IO) {
            EMDKManager.getEMDKManager(context, object : EMDKManager.EMDKListener {
                override fun onOpened(manager: EMDKManager?) {
                    Log.i(TAG, "EMDK opened with manager: $manager")
                    mEmdkManager = manager

                    mEMDKManagerInitCallBacks?.onSuccess()
                }

                override fun onClosed() {
                    Log.w(TAG, "EMDK Manager was closed")
                }
            }).also {
                if (it.statusCode !== EMDKResults.STATUS_CODE.SUCCESS) {
                    Log.e(TAG, "Failed to init: " + it.statusCode)

                    mEMDKManagerInitCallBacks?.onFailed(it.statusString)
                }
            }
        }
    }

    fun release() {
        Log.w(TAG, "About to release the EMDK Manager")
        mEmdkManager?.release()
    }

    fun processEMDKProfile(
        profileName: String,
        profile: String,
        callBacks: EMDKProfileResultCallBacks
    ) {
        Log.d(TAG, "Applying profile...")
        this.mProfileResultCallBacks = callBacks

        val profileManager =
            mEmdkManager?.getInstance(EMDKManager.FEATURE_TYPE.PROFILE) as ProfileManager?

        if (profileManager == null) {
            Log.e(TAG, "Profile Manager is not available!")
            return
        }

        profileProcessScope

        Log.d(TAG, "Processing EMDK profile")
        val params = arrayOfNulls<String>(1)
        params[0] = profile

        profileProcessScope.launch(Dispatchers.IO) {
            profileManager.processProfile(profileName, PROFILE_FLAG.SET, params).also {
                Log.d(TAG, "XML: " + it.statusString)

                if (it.statusCode == STATUS_CODE.CHECK_XML) {
                    mProfileResultCallBacks?.onProfileLoaded()
                } else if (it.statusCode == STATUS_CODE.FAILURE || it.statusCode == STATUS_CODE.SUCCESS) {
                    mProfileResultCallBacks?.onProfileLoadFailed(it.statusString)
                }
            }
        }
    }

    interface EMDKProfileResultCallBacks {
        fun onProfileLoaded()

        fun onProfileLoadFailed(message: String)
    }

    interface EMDKManagerInitCallBacks {
        fun onSuccess()

        fun onFailed(message: String)
    }

    companion object {
        private const val TAG = "EMDKLoader"

        @Volatile
        private var INSTANCE: EMDKLoader? = null

        fun getInstance(): EMDKLoader {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = EMDKLoader()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}