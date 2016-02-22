package com.taibah.busservice.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.taibah.busservice.R;
import com.taibah.busservice.utils.AppGlobals;


public class ManageStudent extends Fragment {

    View convertView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_manage_students, null);
        setHasOptionsMenu(true);

        return convertView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_manage, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_button:
                AppGlobals.replaceFragment(getFragmentManager(), new RegisterStudent());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
