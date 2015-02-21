package be.digitalia.fosdem.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.model.FossasiaEvent;

/**
 * Created by Abhishek on 20/02/15.
 */
public class ScheduleAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<FossasiaEvent> mFossasiaEventList;
    private LayoutInflater mInflater;

    public ScheduleAdapter(Context context, ArrayList<FossasiaEvent> fossasiaEventList) {
        this.mContext = context;
        this.mFossasiaEventList = fossasiaEventList;
    }

    @Override
    public int getCount() {
        return mFossasiaEventList.size();
    }

    @Override
    public FossasiaEvent getItem(int position) {
        return mFossasiaEventList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mFossasiaEventList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


        if (mInflater == null) {
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        View row;
        if (convertView == null) {
            row = mInflater.inflate(R.layout.item_schedule_event, parent, false);
        } else {
            row = convertView;
        }

        ScheduleHolder holder = new ScheduleHolder();
        holder.dateTime = (TextView) row.findViewById(R.id.time);
        holder.title = (TextView) row.findViewById(R.id.text);
        FossasiaEvent fossasiaEvent = getItem(position);
        holder.dateTime.setText(fossasiaEvent.getStartTime() + " - " + fossasiaEvent.getEndTime());
        holder.title.setText(fossasiaEvent.getTitle());
        row.setTag(fossasiaEvent.getId());
        return row;
    }

    public static class ScheduleHolder {
        TextView title;
        TextView dateTime;

    }
}
