/*
 * VenueComparator.java is part of Check Me In
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

import java.util.Comparator;

import fi.foyt.foursquare.api.entities.CompactVenue;

/**
 * Created by quentin on 24/01/14.
 */
public class VenueComparator implements Comparator<CompactVenue> {

    @Override
    public int compare(CompactVenue v1, CompactVenue v2) {
        return v1.getName().toLowerCase().compareTo(v2.getName().toLowerCase());
    }
}
