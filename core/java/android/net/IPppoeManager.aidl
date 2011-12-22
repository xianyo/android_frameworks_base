/**
 * Copyright (c) 2008, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
/* Copyright 2010-2011 Freescale Semiconductor Inc. */

package android.net;

import android.os.WorkSource;

/**
 * Interface that allows controlling and querying Pppoe connectivity.
 *
 * {@hide}
 */
interface IPppoeManager
{

    boolean disconnect();

    boolean reconnect();

    boolean reassociate();

    boolean setPppoeEnabled(boolean enable);

    int getPppoeEnabledState();

    boolean acquirePppoeLock(IBinder lock, int lockType, String tag, in WorkSource ws);

    void updatePppoeLockWorkSource(IBinder lock, in WorkSource ws);

    boolean releasePppoeLock(IBinder lock);
    
    boolean setPppoeAccount(String username, String password);

    boolean removePppoeAccount();
    
}

