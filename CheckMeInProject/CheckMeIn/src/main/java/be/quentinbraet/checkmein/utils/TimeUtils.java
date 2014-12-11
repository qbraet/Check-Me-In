/*
 * TimeUtils.java is part of Check Me In
 *
 * Copyright (c) 2014 Quentin Braet
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: quentinbraet@gmail.com
 */

package be.quentinbraet.checkmein.utils;

import android.content.Context;

import java.util.Calendar;
import java.util.TimeZone;

import be.quentinbraet.checkmein.R;

/**
 * Created by quentin on 16/12/13.
 */
public class TimeUtils {

    public static String textify(Context context, int daysAgo){
        Calendar calendar = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        if (daysAgo == 0) {
            return context.getResources().getString(R.string.today);
        } else if (daysAgo == 1) {
            return context.getResources().getString(R.string.yesterday);
        } else {
            return daysAgo + " " + context.getResources().getString(R.string.days_ago);
        }
    }

    public static Calendar todayMax(){
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR, 23);
        today.set(Calendar.MINUTE, 59);
        today.set(Calendar.SECOND, 59);
        today.setTimeZone(TimeZone.getDefault());
        return today;
    }
}
