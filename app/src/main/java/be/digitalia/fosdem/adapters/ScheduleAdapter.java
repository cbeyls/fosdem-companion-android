package be.digitalia.fosdem.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.model.Schedule;

/**
 * Created by Abhishek on 20/02/15.
 */
public class ScheduleAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Schedule> mScheduleList;
    private LayoutInflater mInflater;

    public ScheduleAdapter(Context context, ArrayList<Schedule> scheduleList) {
        this.mContext = context;
        this.mScheduleList = scheduleList;
    }

    @Override
    public int getCount() {
        return mScheduleList.size();
    }

    @Override
    public Schedule getItem(int position) {
        return mScheduleList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mScheduleList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


        if (mInflater == null) {
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        View row;
        if (convertView == null) {
            row = mInflater.inflate(R.layout.list_schedule, parent, false);
        } else {
            row = convertView;
        }

        ScheduleHolder holder = new ScheduleHolder();
        holder.dateTime = (TextView) row.findViewById(R.id.schedule_time);
        holder.title = (TextView) row.findViewById(R.id.schedule_title);
        Schedule schedule = getItem(position);
        holder.dateTime.setText(schedule.getTime());
        holder.title.setText(schedule.getTitle());
        row.setTag(schedule.getId());
        return row;
    }

    public static class ScheduleHolder {
        TextView title;
        TextView dateTime;

    }
}
