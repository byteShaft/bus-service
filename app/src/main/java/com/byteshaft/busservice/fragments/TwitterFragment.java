package com.byteshaft.busservice.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import com.byteshaft.busservice.R;
import com.byteshaft.busservice.utils.Helpers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterFragment extends Fragment {

    View convertView;
    ImageView imageViewTweets;
    TextView userTextView;
    Bitmap twitterProfileIcon;
    List<Status> statuses;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_twitter, null);

        imageViewTweets = (ImageView) convertView.findViewById(R.id.iv_tweets_icon);
        userTextView = (TextView) convertView.findViewById(R.id.tv_twitter_account_name);

        return convertView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Helpers.showProgressDialog(getActivity(), "Loading twitter feed...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                ConfigurationBuilder cb = new ConfigurationBuilder();
                cb.setDebugEnabled(true)
                        .setOAuthConsumerKey("16eabEYM1yyerYbmqnm9OSbJK")
                        .setOAuthConsumerSecret("8oCNlQD221N9gPUGFpADF4WUD6eUBqSNegxMzOKOblG0kDjghn")
                        .setOAuthAccessToken("3184467546-WY7Z0yUUc2RbCkKS6QWAqamVtlIOr1iV8bW6lcS")
                        .setOAuthAccessTokenSecret("tuz8b3Tt2dlYe2eK7pSzBumD6FklaynchGwEmQIJhRbE6");
                TwitterFactory tf = new TwitterFactory(cb.build());
                Twitter twitter = tf.getInstance();
                Paging paging = new Paging(1, 10);

                try {
                    final User user = twitter.showUser("taibahuen");
                    URL url = new URL(user.getBiggerProfileImageURL());
                    twitterProfileIcon = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    statuses = twitter.getUserTimeline("taibahuen", paging);
                    final tweetsArrayAdapter tweetsAdapter = new tweetsArrayAdapter(
                            getActivity().getApplicationContext(), R.layout.tweets_row, statuses);

                    final ListView listview =(ListView) convertView.findViewById(R.id.lv_tweets);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listview.setAdapter(tweetsAdapter);
                            imageViewTweets.setImageBitmap(twitterProfileIcon);
                            userTextView.setText("@" + user.getScreenName());
                        }
                    });
                } catch (TwitterException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Helpers.dismissProgressDialog();
            }
        }).start();
   }

    private class tweetsArrayAdapter extends ArrayAdapter<List> {

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                Context context = getActivity().getApplicationContext();
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.tweets_row, parent, false);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.tv_tweet);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Status status = (Status) getItem(position);
            holder.title.setText(status.getText());
            return convertView;
        }

        public tweetsArrayAdapter(Context context, int resource, List objects) {
            super(context, resource, objects);

        }
    }

    static class ViewHolder {
        public TextView title;
    }
}
