/*
 * AddSearchAdapter.java is part of Check Me In
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
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import be.quentinbraet.checkmein.Config;
import be.quentinbraet.checkmein.R;
import be.quentinbraet.checkmein.ui.activities.AddConfigActivity_;
import be.quentinbraet.checkmein.utils.VenueComparator;
import fi.foyt.foursquare.api.entities.CompactVenue;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created by quentin on 19/10/13.
 */
public class AddSearchAdapter extends BaseAdapter implements StickyListHeadersAdapter {

    private final LayoutInflater inflater;
    private final Context context;
    private List<CompactVenue> mayors;
    private List<CompactVenue> history;
    private List<CompactVenue> tracked;

    public AddSearchAdapter(Context context){
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    public List<CompactVenue> getMayors() {
        if(mayors == null) return new LinkedList<CompactVenue>();
        return mayors;
    }

    public void setMayors(CompactVenue[] mayors) {
        this.mayors = new LinkedList<CompactVenue>();

        if(mayors != null){
            for(CompactVenue venue : mayors){
                this.mayors.add(venue);
                Collections.sort(this.mayors, new VenueComparator());
            }
        }
    }

    public List<CompactVenue> getHistory() {
        if(history == null) return new LinkedList<CompactVenue>();
        return history;
    }

    public void setHistory(CompactVenue[] history) {
        this.history = new LinkedList<CompactVenue>();

        if(history != null){
            for(CompactVenue venue : history){
                this.history.add(venue);
                Collections.sort(this.history, new VenueComparator());
            }
        }
        notifyDataSetChanged();
    }

    public List<CompactVenue> getTracked() {
        if(tracked == null) return new ArrayList<CompactVenue>();
        return tracked;
    }

    public void setTracked(List<CompactVenue> tracked) {
        this.tracked = tracked;
        notifyDataSetChanged();
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = inflater.inflate(R.layout.item_header, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.text);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }
        //set header text as first char in name
        String headerText;
        if(position < getMayors().size()){
            headerText = context.getResources().getString(R.string.mayorships);
        }else{
            headerText = context.getResources().getString(R.string.checkin_history);
        }

        holder.text.setText(headerText);
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        if(position < getMayors().size()) return 0;
        return 1;
    }

    @Override
    public int getCount() {
        return getMayors().size() + getHistory().size();
    }

    @Override
    public Object getItem(int position) {
        if(position < getMayors().size()) return getMayors().get(position);
        return getHistory().get(position - getMayors().size());
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
            convertView = inflater.inflate(R.layout.item_venue_add, parent, false);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.add = (ImageButton) convertView.findViewById(R.id.add);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final CompactVenue venue;

        if(position < getMayors().size()){
            venue = getMayors().get(position);
        }else{
            venue = getHistory().get(position - getMayors().size());
        }

        holder.name.setText(venue.getName());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://m.foursquare.com/venue/" + venue.getId() + "?ref=" + Config.CLIENT_ID));
                context.startActivity(intent);
            }
        });

        holder.add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, AddConfigActivity_.class);
                intent.putExtra("venue", venue);
                context.startActivity(intent);
            }
        });

        boolean contained = false;
        for(CompactVenue cv : getTracked()){
            if(cv.getId().equals(venue.getId())){
                contained = true;
                break;
            }
        }

        if(contained){
            holder.add.setContentDescription(context.getResources().getString(R.string.config));
            holder.add.setImageResource(R.drawable.ic_action_edit_black);
        }else{
            holder.add.setContentDescription(context.getResources().getString(R.string.add));
            holder.add.setImageResource(R.drawable.ic_action_new_black);
        }

        return convertView;
    }

    class HeaderViewHolder {
        TextView text;
    }

    class ViewHolder {
        TextView name;
        ImageButton add;
    }

}
