/*
 * HomeVenueAdapter.java is part of Check Me In
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

package be.quentinbraet.checkmein.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import be.quentinbraet.checkmein.R;
import be.quentinbraet.checkmein.ui.activities.AddConfigActivity_;
import be.quentinbraet.checkmein.utils.VenueComparator;
import fi.foyt.foursquare.api.entities.CompactVenue;

/**
 * Created by quentin on 21/10/13.
 */
public class HomeVenueAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final Context context;
    List<CompactVenue> items;

    public HomeVenueAdapter(Context context){
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    public void setVenues(List<CompactVenue> venues){
        this.items = venues;
        Collections.sort(this.items, new VenueComparator());
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if(items == null) return 0;
        return items.size();
    }

    @Override
    public CompactVenue getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.item_venue_home, parent, false);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final CompactVenue venue = getItem(position);

        holder.name.setText(venue.getName());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, AddConfigActivity_.class);
                intent.putExtra("venue", venue);
                context.startActivity(intent);
            }
        });

        return convertView;
    }

    class ViewHolder {
        TextView name;
    }
}
