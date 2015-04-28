package com.example.lgf.bluetouch;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collection;
import java.util.List;


/**
 * Created by LGF on 2015/4/22.
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private Context context;
    private List<BluetoothDevice> list;
     private View.OnClickListener listener;
    public DeviceAdapter(Context context, List<BluetoothDevice> list) {
        this.context = context;
        this.list = list;


        if (context instanceof  View.OnClickListener){
            listener= ((View.OnClickListener) context);

        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.item, viewGroup, false);
     view.setOnClickListener(listener);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        BluetoothDevice device = list.get(i);
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            viewHolder.name.setTextColor(Color.RED);

        } else {
            viewHolder.name.setTextColor(Color.BLACK);
        }

        viewHolder.name.setText(device.getName());
        viewHolder.address.setText(device.getAddress());

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void addAll(Collection<? extends BluetoothDevice> devices) {
        int size = list.size();
        list.addAll(devices);
        notifyItemRangeInserted(size, devices.size());
    }

    public void add(BluetoothDevice device) {


        if (!list.contains(device)){

            list.add(0, device);
            notifyItemInserted(0);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView name, address;

        public ViewHolder(View itemView) {

            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name);
            address = (TextView) itemView.findViewById(R.id.address);
        }
    }

     public BluetoothDevice getItem(int position){
         return list.get(position);
     }
}
