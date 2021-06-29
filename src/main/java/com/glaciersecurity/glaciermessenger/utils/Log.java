/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.glaciersecurity.glaciermessenger.utils;

import com.glaciersecurity.glaciermessenger.Config;

/**
 The logLevel variable controls what is seen when using logcat while using the app.
 Available values are 0 through 5, with each level providing more detail than the previous level.
 Available logging levels are as follows:

 logLevel = 0    No logging (Use this for a production release)
 logLevel = 1    Error logging (Only shows errors)
 logLevel = 2    Warning logging (Shows warnings and errors)
 logLevel = 3    Info logging (Shows info, warnings, and errors)
 logLevel = 4    Debug logging (Shows debug logging, as well as info, warnings, and errors)
 logLevel = 5    Verbose logging (Essentially shows all available logging)
 */

public class Log {
    private static int logLevel = Config.logLevel;  // SET THIS TO ZERO FOR PRODUCTION RELEASES

    /**
     * Change current logging level
     * @param level new log level 1 <= level <= 6
     */
    public static void setLogLevel(int level) {
        logLevel = level;
    }

    /**
     * Get the current log level
     * @return the log level
     */
    public static int getLogLevel() {
        return logLevel;
    }

    /**
     * Log verbose
     * @param tag Tag for this log
     * @param msg Msg for this log
     */
    public static void v(String tag, String msg) {
        if(logLevel >= 5) {
            android.util.Log.v(tag, msg);
        }
    }

    /**
     * Log verbose
     * @param tag Tag for this log
     * @param msg Msg for this log
     * @param tr Error to serialize in log
     */
    public static void v(String tag, String msg, Throwable tr) {
        if(logLevel >= 5) {
            android.util.Log.v(tag, msg, tr);
        }
    }

    /**
     * Log debug
     * @param tag Tag for this log
     * @param msg Msg for this log
     */
    public static void d(String tag, String msg) {
        if(logLevel >= 4) {
            android.util.Log.d(tag, msg);
        }
    }

    /**
     * Log debug
     * @param tag Tag for this log
     * @param msg Msg for this log
     * @param tr Error to serialize in log
     */
    public static void d(String tag, String msg, Throwable tr) {
        if(logLevel >= 4) {
            android.util.Log.d(tag, msg, tr);
        }
    }

    /**
     * Log info
     * @param tag Tag for this log
     * @param msg Msg for this log
     */
    public static void i(String tag, String msg) {
        if(logLevel >= 3) {
            android.util.Log.i(tag, msg);
        }
    }

    /**
     * Log info
     * @param tag Tag for this log
     * @param msg Msg for this log
     * @param tr Error to serialize in log
     */
    static void i(String tag, String msg, Throwable tr) {
        if(logLevel >= 3) {
            android.util.Log.i(tag, msg, tr);
        }
    }

    /**
     * Log warning
     * @param tag Tag for this log
     * @param msg Msg for this log
     */
    public static void w(String tag, String msg) {
        if(logLevel >= 2) {
            android.util.Log.w(tag, msg);
        }
    }

    /**
     * Log warning
     * @param tag Tag for this log
     * @param msg Msg for this log
     * @param tr Error to serialize in log
     */
    public static void w(String tag, String msg, Throwable tr) {
        if(logLevel >= 2) {
            android.util.Log.w(tag, msg, tr);
        }
    }

    /**
     * Log error
     * @param tag Tag for this log
     * @param msg Msg for this log
     */
    public static void e(String tag, String msg) {
        if(logLevel >= 1) {
            android.util.Log.e(tag, msg);
        }
    }

    /**
     * Log error
     * @param tag Tag for this log
     * @param msg Msg for this log
     * @param tr Error to serialize in log
     */
    public static void e(String tag, String msg, Throwable tr) {
        if(logLevel >= 1) {
            android.util.Log.e(tag, msg, tr);
        }
    }

}
