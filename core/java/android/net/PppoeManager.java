/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* Copyright 2010-2011 Freescale Semiconductor Inc. */

package android.net;

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.net.DhcpInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.RemoteException;
import android.os.WorkSource;

import java.util.List;

/**
 * This class provides the primary API for managing all aspects of Pppoe
 * connectivity. Get an instance of this class by calling
 * {@link android.content.Context#getSystemService(String) Context.getSystemService(Context.PPPOE_SERVICE)}.

 * It deals with several categories of items:
 * <ul>
 * <li>The list of configured networks. The list can be viewed and updated,
 * and attributes of individual entries can be modified.</li>
 * <li>The currently active Pppoe network, if any. Connectivity can be
 * established or torn down, and dynamic information about the state of
 * the network can be queried.</li>
 * <li>Results of access point scans, containing enough information to
 * make decisions about what access point to connect to.</li>
 * <li>It defines the names of various Intent actions that are broadcast
 * upon any sort of change in Pppoe state.
 * </ul>
 * This is the API to use when performing Pppoe specific operations. To
 * perform operations that pertain to network connectivity at an abstract
 * level, use {@link android.net.ConnectivityManager}.
 */
public class PppoeManager {

    // Supplicant error codes:
    /**
     * The error code if there was a problem authenticating.
     */
    public static final int ERROR_AUTHENTICATING = 1;

    /**
     * Broadcast intent action indicating that Pppoe has been enabled, disabled,
     * enabling, disabling, or unknown. One extra provides this state as an int.
     * Another extra provides the previous state, if available.
     * 
     * @see #EXTRA_PPPOE_STATE
     * @see #EXTRA_PREVIOUS_PPPOE_STATE
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String PPPOE_STATE_CHANGED_ACTION =
        "android.net.PPPOE_STATE_CHANGED";
    /**
     * The lookup key for an int that indicates whether Pppoe is enabled,
     * disabled, enabling, disabling, or unknown.  Retrieve it with
     * {@link android.content.Intent#getIntExtra(String,int)}.
     * 
     * @see #PPPOE_STATE_DISABLED
     * @see #PPPOE_STATE_DISABLING
     * @see #PPPOE_STATE_ENABLED
     * @see #PPPOE_STATE_ENABLING
     * @see #PPPOE_STATE_UNKNOWN
     */
    public static final String EXTRA_PPPOE_STATE = "pppoe_state";
    /**
     * The previous Pppoe state.
     * 
     * @see #EXTRA_PPPOE_STATE
     */
    public static final String EXTRA_PREVIOUS_PPPOE_STATE = "previous_pppoe_state";
    
    /**
     * Pppoe is currently being disabled. The state will change to {@link #PPPOE_STATE_DISABLED} if
     * it finishes successfully.
     * 
     * @see #PPPOE_STATE_CHANGED_ACTION
     * @see #getPppoeState()
     */
    public static final int PPPOE_STATE_DISABLING = 0;
    /**
     * Pppoe is disabled.
     * 
     * @see #PPPOE_STATE_CHANGED_ACTION
     * @see #getPppoeState()
     */
    public static final int PPPOE_STATE_DISABLED = 1;
    /**
     * Pppoe is currently being enabled. The state will change to {@link #PPPOE_STATE_ENABLED} if
     * it finishes successfully.
     * 
     * @see #PPPOE_STATE_CHANGED_ACTION
     * @see #getPppoeState()
     */
    public static final int PPPOE_STATE_ENABLING = 2;
    /**
     * Pppoe is enabled.
     * 
     * @see #PPPOE_STATE_CHANGED_ACTION
     * @see #getPppoeState()
     */
    public static final int PPPOE_STATE_ENABLED = 3;
    /**
     * Pppoe is in an unknown state. This state will occur when an error happens while enabling
     * or disabling.
     * 
     * @see #PPPOE_STATE_CHANGED_ACTION
     * @see #getPppoeState()
     */
    public static final int PPPOE_STATE_UNKNOWN = 4;


    public static final int PPPOE_STATE_CHECK_CONNNECT = 5;
    public static final int PPPOE_STATE_NO_ACCOUNT = 6;    
    public static final int PPPOE_STATE_MODEM_HUNGUP = 7;  
    public static final int PPPOE_STATE_ACCOUNT_UNCORRECT = 8;    
    /**
     * Broadcast intent action indicating that the state of Pppoe connectivity
     * has changed. One extra provides the new state
     * in the form of a {@link android.net.NetworkInfo} object. If the new state is
     * CONNECTED, a second extra may provide the BSSID of the access point,
     * as a {@code String}.
     * @see #EXTRA_NETWORK_INFO
     * @see #EXTRA_BSSID
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String NETWORK_STATE_CHANGED_ACTION = "android.net.pppoe.STATE_CHANGE";
    /**
     * The lookup key for a {@link android.net.NetworkInfo} object associated with the
     * Pppoe network. Retrieve with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     */
    public static final String EXTRA_NETWORK_INFO = "networkInfo";

    IPppoeManager mService;
    Handler mHandler;
    
    /* Maximum number of active locks we allow.
     * This limit was added to prevent apps from creating a ridiculous number
     * of locks and crashing the system by overflowing the global ref table.
     */
    private static final int MAX_ACTIVE_LOCKS = 50;
    
    /* Number of currently active PppoeLocks */
    private int mActiveLockCount;

    /**
     * Create a new PppoeManager instance.
     * Applications will almost always want to use
     * {@link android.content.Context#getSystemService Context.getSystemService()} to retrieve
     * the standard {@link android.content.Context#PPPOE_SERVICE Context.PPPOE_SERVICE}.
     * @param service the Binder interface
     * @param handler target for messages
     * @hide - hide this because it takes in a parameter of type IPppoeManager, which
     * is a system private class.
     */
    public PppoeManager(IPppoeManager service, Handler handler) {
        mService = service;
        mHandler = handler;
    }


    /**
     * Remove the specified network from the list of configured networks.
     * This may result in the asynchronous delivery of state change
     * events.
     * @param netId the integer that identifies the network configuration
     * to the supplicant
     * @return {@code true} if the operation succeeded
     */
    public boolean removeNetwork(int netId) {

        return false;
    }

    /**
     * Allow a previously configured network to be associated with. If
     * <code>disableOthers</code> is true, then all other configured
     * networks are disabled, and an attempt to connect to the selected
     * network is initiated. This may result in the asynchronous delivery
     * of state change events.
     * @param netId the ID of the network in the list of configured networks
     * @param disableOthers if true, disable all other networks. The way to
     * select a particular network to connect to is specify {@code true}
     * for this parameter.
     * @return {@code true} if the operation succeeded
     */
    public boolean enableNetwork(int netId, boolean disableOthers) {
            return false;
    }

    /**
     * Disable a configured network. The specified network will not be
     * a candidate for associating. This may result in the asynchronous
     * delivery of state change events.
     * @param netId the ID of the network as returned by .
     * @return {@code true} if the operation succeeded
     */
    public boolean disableNetwork(int netId) {
            return false;
    }

    /**
     * Disassociate from the currently active access point. This may result
     * in the asynchronous delivery of state change events.
     * @return {@code true} if the operation succeeded
     */
    public boolean disconnect() {
        try {
            return mService.disconnect();
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Reconnect to the currently active access point, if we are currently
     * disconnected. This may result in the asynchronous delivery of state
     * change events.
     * @return {@code true} if the operation succeeded
     */
    public boolean reconnect() {
        try {
            return mService.reconnect();
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Reconnect to the currently active access point, even if we are already
     * connected. This may result in the asynchronous delivery of state
     * change events.
     * @return {@code true} if the operation succeeded
     */
    public boolean reassociate() {
        try {
            return mService.reassociate();
        } catch (RemoteException e) {
            return false;
        }
    }


    /**
     * Enable or disable Pppoe.
     * @param enabled {@code true} to enable, {@code false} to disable.
     * @return {@code true} if the operation succeeds (or if the existing state
     *         is the same as the requested state).
     */
    public boolean setPppoeEnabled(boolean enabled) {
        try {
            return mService.setPppoeEnabled(enabled);
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Gets the Pppoe enabled state.
     * @return One of {@link #PPPOE_STATE_DISABLED},
     *         {@link #PPPOE_STATE_DISABLING}, {@link #PPPOE_STATE_ENABLED},
     *         {@link #PPPOE_STATE_ENABLING}, {@link #PPPOE_STATE_UNKNOWN}
     * @see #isPppoeEnabled()
     */
    public int getPppoeState() {
        try {
            return mService.getPppoeEnabledState();
        } catch (RemoteException e) {
            return PPPOE_STATE_UNKNOWN;
        }
    }
    
    /**
     * Return whether Pppoe is enabled or disabled. 
     * @return {@code true} if Pppoe is enabled
     * @see #getPppoeState()
     */
    public boolean isPppoeEnabled() {
        return getPppoeState() == PPPOE_STATE_ENABLED;
    }
    

    public boolean setPppoeAccount(String username, String password) {
        try {
            return mService.setPppoeAccount(username, password);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean removePppoeAccount() {
        try {
            return mService.removePppoeAccount();
        } catch (RemoteException e) {
            return false;
        }
    }
    
    /**
     * Allows an application to keep the Pppoe radio awake.
     * Normally the Pppoe radio may turn off when the user has not used the device in a while.
     * Acquiring a PppoeLock will keep the radio on until the lock is released.  Multiple 
     * applications may hold PppoeLocks, and the radio will only be allowed to turn off when no
     * PppoeLocks are held in any application.
     *
     * Before using a PppoeLock, consider carefully if your application requires Pppoe access, or
     * could function over a mobile network, if available.  A program that needs to download large
     * files should hold a PppoeLock to ensure that the download will complete, but a program whose
     * network usage is occasional or low-bandwidth should not hold a PppoeLock to avoid adversely
     * affecting battery life.
     *
     * Note that PppoeLocks cannot override the user-level "Pppoe Enabled" setting, nor Airplane
     * Mode.  They simply keep the radio from turning off when Pppoe is already on but the device
     * is idle.
     */
    public class PppoeLock {
        private String mTag;
        private final IBinder mBinder;
        private int mRefCount;
        int mLockType;
        private boolean mRefCounted;
        private boolean mHeld;
        private WorkSource mWorkSource;

        private PppoeLock(int lockType, String tag) {
            mTag = tag;
            mLockType = lockType;
            mBinder = new Binder();
            mRefCount = 0;
            mRefCounted = true;
            mHeld = false;
        }

        /**
         * Locks the Pppoe radio on until {@link #release} is called.
         *
         * If this PppoeLock is reference-counted, each call to {@code acquire} will increment the
         * reference count, and the radio will remain locked as long as the reference count is 
         * above zero.
         *
         * If this PppoeLock is not reference-counted, the first call to {@code acquire} will lock
         * the radio, but subsequent calls will be ignored.  Only one call to {@link #release}
         * will be required, regardless of the number of times that {@code acquire} is called.
         */
        public void acquire() {
            synchronized (mBinder) {
                if (mRefCounted ? (++mRefCount > 0) : (!mHeld)) {
                    try {
                        mService.acquirePppoeLock(mBinder, mLockType, mTag, mWorkSource);
                        synchronized (PppoeManager.this) {
                            if (mActiveLockCount >= MAX_ACTIVE_LOCKS) {
                                mService.releasePppoeLock(mBinder);
                                throw new UnsupportedOperationException(
                                            "Exceeded maximum number of Pppoe locks");
                            }
                            mActiveLockCount++;
                        }
                    } catch (RemoteException ignore) {
                    }
                    mHeld = true;
                }
            }
        }

        /**
         * Unlocks the Pppoe radio, allowing it to turn off when the device is idle.
         *
         * If this PppoeLock is reference-counted, each call to {@code release} will decrement the
         * reference count, and the radio will be unlocked only when the reference count reaches
         * zero.  If the reference count goes below zero (that is, if {@code release} is called
         * a greater number of times than {@link #acquire}), an exception is thrown.
         *
         * If this PppoeLock is not reference-counted, the first call to {@code release} (after
         * the radio was locked using {@link #acquire}) will unlock the radio, and subsequent
         * calls will be ignored.
         */
        public void release() {
            synchronized (mBinder) {
                if (mRefCounted ? (--mRefCount == 0) : (mHeld)) {
                    try {
                        mService.releasePppoeLock(mBinder);
                        synchronized (PppoeManager.this) {
                            mActiveLockCount--;
                        }
                    } catch (RemoteException ignore) {
                    }
                    mHeld = false;
                }
                if (mRefCount < 0) {
                    throw new RuntimeException("PppoeLock under-locked " + mTag);
                }
            }
        }

        /**
         * Controls whether this is a reference-counted or non-reference-counted PppoeLock.
         *
         * Reference-counted PppoeLocks keep track of the number of calls to {@link #acquire} and
         * {@link #release}, and only allow the radio to sleep when every call to {@link #acquire}
         * has been balanced with a call to {@link #release}.  Non-reference-counted PppoeLocks
         * lock the radio whenever {@link #acquire} is called and it is unlocked, and unlock the
         * radio whenever {@link #release} is called and it is locked.
         *
         * @param refCounted true if this PppoeLock should keep a reference count
         */
        public void setReferenceCounted(boolean refCounted) {
            mRefCounted = refCounted;
        }

        /**
         * Checks whether this PppoeLock is currently held.
         *
         * @return true if this PppoeLock is held, false otherwise
         */
        public boolean isHeld() {
            synchronized (mBinder) {
                return mHeld;
            }
        }

        public void setWorkSource(WorkSource ws) {
            synchronized (mBinder) {
                if (ws != null && ws.size() == 0) {
                    ws = null;
                }
                boolean changed = true;
                if (ws == null) {
                    mWorkSource = null;
                } else if (mWorkSource == null) {
                    changed = mWorkSource != null;
                    mWorkSource = new WorkSource(ws);
                } else {
                    changed = mWorkSource.diff(ws);
                    if (changed) {
                        mWorkSource.set(ws);
                    }
                }
                if (changed && mHeld) {
                    try {
                        mService.updatePppoeLockWorkSource(mBinder, mWorkSource);
                    } catch (RemoteException e) {
                    }
                }
            }
        }

        public String toString() {
            String s1, s2, s3;
            synchronized (mBinder) {
                s1 = Integer.toHexString(System.identityHashCode(this));
                s2 = mHeld ? "held; " : "";
                if (mRefCounted) {
                    s3 = "refcounted: refcount = " + mRefCount;
                } else {
                    s3 = "not refcounted";
                }
                return "PppoeLock{ " + s1 + "; " + s2 + s3 + " }";
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            synchronized (mBinder) {
                if (mHeld) {
                    try {
                        mService.releasePppoeLock(mBinder);
                        synchronized (PppoeManager.this) {
                            mActiveLockCount--;
                        }
                    } catch (RemoteException ignore) {
                    }
                }
            }
        }
    }

    /**
     * Creates a new PppoeLock.
     *
     * @param lockType the type of lock to create. See ,
     * and  for descriptions of the types of Pppoe locks.
     *
     * @param tag a tag for the PppoeLock to identify it in debugging messages.  This string is 
     *            never shown to the user under normal conditions, but should be descriptive 
     *            enough to identify your application and the specific PppoeLock within it, if it
     *            holds multiple PppoeLocks.
     *
     * @return a new, unacquired PppoeLock with the given tag.
     *
     * @see PppoeLock
     */
    public PppoeLock createPppoeLock(int lockType, String tag) {
        return new PppoeLock(lockType, tag);
    }
    

}
