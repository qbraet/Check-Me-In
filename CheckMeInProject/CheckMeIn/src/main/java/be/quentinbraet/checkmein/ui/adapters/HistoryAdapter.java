/*
 * HistoryAdapter.java is part of Check Me In
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import be.quentinbraet.checkmein.R;
import be.quentinbraet.checkmein.ui.activities.AddConfigActivity_;
import be.quentinbraet.checkmein.utils.StorageUtils;
import be.quentinbraet.checkmein.utils.TimeUtils;
import fi.foyt.foursquare.api.entities.Checkin;
import fi.foyt.foursquare.api.entities.CompactVenue;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created by quentin on 16/12/13.
 */
public class HistoryAdapter extends BaseAdapter implements StickyListHeadersAdapter {

    private final Context context;
    private final LayoutInflater inflater;
    private String[] tracked;
    private Checkin[] checkins;

    public static final long DAY_IN_S = 60 * 60 * 24;

    public HistoryAdapter(Context context){
        this.context = context;
        inflater = LayoutInflater.from(context);
        tracked = StorageUtils.getInstance().loadCompactVenueIds(context);
    }

    public void updateTracked(){
        tracked = StorageUtils.getInstance().loadCompactVenueIds(context);
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

        Calendar now = Calendar.getInstance();
        long dayNow = now.get(Calendar.DAY_OF_YEAR) + (now.get(Calendar.YEAR) * 365);
        Log.d("CMI_now","now " + dayNow);

        Calendar checkin = Calendar.getInstance();
        checkin.setTimeInMillis(checkins[position].getCreatedAt() * 1000);
        long dayThen = checkin.get(Calendar.DAY_OF_YEAR) + (checkin.get(Calendar.YEAR) * 365);
        Log.d("CMI_now","then " + dayThen);
        int daysAgo = (int) (dayNow - dayThen);

        holder.text.setText(TimeUtils.textify(context, daysAgo));
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {

        Calendar now = Calendar.getInstance();
        long dayNow = now.get(Calendar.DAY_OF_YEAR) * now.get(Calendar.YEAR);

        Calendar checkin = Calendar.getInstance();
        checkin.setTimeInMillis(checkins[position].getCreatedAt() * 1000);
        long dayThen = checkin.get(Calendar.DAY_OF_YEAR) * checkin.get(Calendar.YEAR);

        return dayNow - dayThen;
    }

    @Override
    public int getCount() {
        if(checkins == null) return 0;
        return checkins.length;
    }

    @Override
    public Checkin getItem(int i) {
        return checkins[i];
    }

    public Checkin[] getCheckins(){
        if(checkins == null) return new Checkin[0];
        return checkins;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.item_history, parent, false);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.time = (TextView) convertView.findViewById(R.id.time);
            holder.logo = (ImageView) convertView.findViewById(R.id.logo);
            holder.add = (ImageButton) convertView.findViewById(R.id.add);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Checkin checkin = getItem(position);
        final CompactVenue venue = checkin.getVenue();

        if(venue != null){
            holder.name.setText(checkin.getVenue().getName());
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, AddConfigActivity_.class);
                    intent.putExtra("venue", venue);
                    context.startActivity(intent);
                }
            });
        }else{
            holder.name.setText(checkin.getId());
        }

        if(checkin.getSource().getName().equals("CheckMeIn")){
            holder.logo.setVisibility(View.VISIBLE);
            holder.add.setVisibility(View.GONE);
        }else{
            holder.logo.setVisibility(View.GONE);
            holder.add.setVisibility(View.VISIBLE);
            holder.add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, AddConfigActivity_.class);
                    intent.putExtra("venue", venue);
                    context.startActivity(intent);
                }
            });

            if(venue != null && tracked != null){

                boolean contained = false;
                for(String id : tracked){
                    if(id.equals(venue.getId())){
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

            }
        }

        Date date = new Date(checkin.getCreatedAt()*1000);
        DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
        //DateFormat format = new SimpleDateFormat("H:mm");
        holder.time.setText(format.format(date));

        return convertView;
    }

    public void setCheckins(Checkin[] checkins) {
        this.checkins = checkins;
        notifyDataSetChanged();
    }

    class HeaderViewHolder {
        TextView text;
    }

    class ViewHolder {
        TextView name;
        TextView time;
        ImageView logo;
        ImageButton add;
    }
}
