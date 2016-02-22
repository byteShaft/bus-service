package com.byteshaft.busservice;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.byteshaft.busservice.Helpers.WebServiceHelpers;
import com.byteshaft.busservice.fragments.ChangePasswordFragment;
import com.byteshaft.busservice.fragments.ContactFragment;
import com.byteshaft.busservice.fragments.HomeFragment;
import com.byteshaft.busservice.fragments.ManageDrivers;
import com.byteshaft.busservice.fragments.ManageRoutes;
import com.byteshaft.busservice.fragments.ManageStudent;
import com.byteshaft.busservice.fragments.MapsFragment;
import com.byteshaft.busservice.fragments.ScheduleFragment;
import com.byteshaft.busservice.fragments.TwitterFragment;
import com.byteshaft.busservice.utils.AppGlobals;
import com.byteshaft.busservice.utils.Helpers;

import org.json.JSONException;

import java.io.IOException;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = WebServiceHelpers.getToken();
                    System.out.println(token);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();


        if (AppGlobals.isVirgin()) {
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(i);
            return;
        }

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.container_main, new HomeFragment());
        tx.commit();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);

        if (AppGlobals.getUserType() == 0) {
            navigationView.getMenu().getItem(1).setVisible(false);
            navigationView.getMenu().getItem(2).setVisible(false);
            navigationView.getMenu().getItem(3).setVisible(true);
            navigationView.getMenu().getItem(4).setVisible(true);
            navigationView.getMenu().getItem(5).setVisible(true);
        } else if (AppGlobals.getUserType() == 1) {
            navigationView.getMenu().getItem(3).setVisible(false);
            navigationView.getMenu().getItem(4).setVisible(false);
            navigationView.getMenu().getItem(5).setVisible(false);
        } else if (AppGlobals.getUserType() == 2) {
            navigationView.getMenu().getItem(2).setVisible(false);
            navigationView.getMenu().getItem(3).setVisible(false);
            navigationView.getMenu().getItem(4).setVisible(false);
            navigationView.getMenu().getItem(5).setVisible(false);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void selectDrawerItem(MenuItem menuItem) {
        boolean logoutCheck = false;
        fragment = null;
        Class fragmentClass = null;
        switch (menuItem.getItemId()) {
            case R.id.nav_home:
                fragmentClass = HomeFragment.class;
                break;
            case R.id.nav_map:
                fragmentClass = MapsFragment.class;
                break;
            case R.id.nav_schedule:
                fragmentClass = ScheduleFragment.class;
                break;
            case R.id.nav_manage_routes:
                fragmentClass = ManageRoutes.class;
                break;
            case R.id.nav_manage_drivers:
                fragmentClass = ManageDrivers.class;
                break;
            case R.id.nav_manage_students:
                fragmentClass = ManageStudent.class;
                break;
            case R.id.nav_change_password:
                fragmentClass = ChangePasswordFragment.class;
                break;
            case R.id.nav_logout:
                logoutCheck = true;
                showLogoutDialog();
                break;
            case R.id.nav_twitter:
                fragmentClass = TwitterFragment.class;
                break;
            case R.id.nav_contact:
                fragmentClass = ContactFragment.class;
                break;
            default:
                fragmentClass = HomeFragment.class;
        }

        if (!logoutCheck) {
            try {
                fragment = (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            menuItem.setCheckable(true);
            setTitle(menuItem.getTitle());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction().replace(R.id.container_main, fragment).commit();
                }
            }, 300);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        selectDrawerItem(item);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void showLogoutDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                MainActivity.this);
        alertDialogBuilder.setTitle("Logout");
        alertDialogBuilder
                .setMessage("Are you sure?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        logout();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void logout() {

        Helpers.showProgressDialog(MainActivity.this, "Logging Out...");

        // TODO: Implement logout logic here.

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        AppGlobals.setVirgin(true);
                        Helpers.dismissProgressDialog();
                        finish();
                    }
                }, 2000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}