package com.example.nan.ssprocess.adapter;

import android.annotation.SuppressLint;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.nan.ssprocess.R;
import com.example.nan.ssprocess.bean.basic.InstallPlanData;

import java.util.ArrayList;

/**
 * Created by nan on 2018/2/13.
 */

public class InstallPlanAdapter extends RecyclerView.Adapter {

    private static String TAG = "nlgInstallPlanAdapter";
    private ArrayList<InstallPlanData> mInstallPlanAdapter;
    private InstallPlanAdapter.OnItemClickListener itemClickListener = null;

    public InstallPlanAdapter(ArrayList<InstallPlanData> list) {
        mInstallPlanAdapter = list;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_install_plan,parent,false);
        return new InstallPlanAdapter.ItemView(view);

    }

    /**
     * 绑定数据
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        final InstallPlanAdapter.ItemView itemView = (InstallPlanAdapter.ItemView) holder;
        //itemView.setIsRecyclable(false);//禁止复用
        if (mInstallPlanAdapter!=null && !mInstallPlanAdapter.isEmpty() && position < mInstallPlanAdapter.size()) {
            itemView.orderNumberTv.setText(mInstallPlanAdapter.get(position).getOrderNum());
            itemView.nameplateTv.setText(mInstallPlanAdapter.get(position).getNameplate());
            itemView.headNumberTv.setText(mInstallPlanAdapter.get(position).getHeadNum());
            itemView.needleNumberTv.setText(mInstallPlanAdapter.get(position).getNeedleNum());
            itemView.locationTv.setText(mInstallPlanAdapter.get(position).getLocation());

            itemView.installPlanLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (itemClickListener != null) {
                        itemClickListener.onItemClick(position);
                    }
                }
            });
        }else {
            Log.d(TAG, "onBindViewHolder: 没有获取到list数据");
        }
    }

    @Override
    public int getItemCount() {
        return mInstallPlanAdapter.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mInstallPlanAdapter.size()) {
            return 1;
        } else {
            return 0;
        }
    }

    public class ItemView extends RecyclerView.ViewHolder {
        CardView installPlanLayout;
        TextView orderNumberTv;
        TextView nameplateTv;
        TextView headNumberTv;
        TextView needleNumberTv;
        TextView locationTv;

        ItemView(View itemView) {
            super(itemView);
            installPlanLayout = itemView.findViewById(R.id.item_install_plan_layout);
            orderNumberTv = itemView.findViewById(R.id.order_number_tv);
            nameplateTv = itemView.findViewById(R.id.nameplate_tv);
            headNumberTv = itemView.findViewById(R.id.head_number_tv);
            needleNumberTv = itemView.findViewById(R.id.needle_number_tv);
            locationTv = itemView.findViewById(R.id.location_tv);
        }
    }
    public void setProcessList(ArrayList<InstallPlanData> list) {
        mInstallPlanAdapter.clear();
        mInstallPlanAdapter.addAll(list);
    }

    /**
     * 点击事件接口
     */
    public interface OnItemClickListener{
        void onItemClick(int position);
    }

    /**
     * 设置点击事件的方法
     */
    public void setOnItemClickListener(InstallPlanAdapter.OnItemClickListener itemClickListener){
        this.itemClickListener = itemClickListener;
    }
}
