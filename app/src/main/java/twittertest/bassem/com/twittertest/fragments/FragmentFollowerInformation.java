package twittertest.bassem.com.twittertest.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.tweetui.TweetView;

import it.carlom.stikkyheader.core.StikkyHeaderBuilder;
import twittertest.bassem.com.twittertest.Models.Follower;
import twittertest.bassem.com.twittertest.R;
import twittertest.bassem.com.twittertest.Services.UserTimelineService;
import twittertest.bassem.com.twittertest.helpers.Constants;
import twittertest.bassem.com.twittertest.helpers.GsonHelper;
import twittertest.bassem.com.twittertest.helpers.IconAnimator;
import twittertest.bassem.com.twittertest.helpers.MyUtilities;
import twittertest.bassem.com.twittertest.helpers.TwitterHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class FragmentFollowerInformation extends Fragment {
    Follower mFollower;
    FollowerInfoBroadcastReceiver mReceiver;
    Tweet[] tweets;
    final int PAGESIZE = 10;
    ScrollView mScrollView;
    LinearLayout tweetsLinearLayout;
    LinearLayout containerLinearLayout;
    ImageView backgroundImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follower_information, container, false);
        backgroundImageView = (ImageView) view.findViewById(R.id.img_follower_bg);
        mScrollView = (ScrollView) view.findViewById(R.id.scrl_main);
        tweetsLinearLayout = (LinearLayout) view.findViewById(R.id.lnr_tweets);
        containerLinearLayout = (LinearLayout) view.findViewById(R.id.lnr_user_tweets_fragment);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mReceiver = new FollowerInfoBroadcastReceiver();
        tweets = new Tweet[0];
        IntentFilter filter = new IntentFilter(FollowerInfoBroadcastReceiver.PROCESS_USER_INFO_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        getContext().registerReceiver(mReceiver, filter);
        if (getArguments() != null)
            mFollower = getArguments().getParcelable(Constants.FOLLOWER_EXTRA);
        populateFollowerLayout();
    }

    private void populateFollowerLayout() {
        if (mFollower != null) {
            if (mFollower.getBannerBackgroundUrl() != null && mFollower.getBannerBackgroundUrl().isEmpty() == false)
                Glide.with(getContext()).load(mFollower.getBannerBackgroundUrl()).into(backgroundImageView);
            if (MyUtilities.checkForInternet(getContext())) {
                getTimeLine();
            } else {
                Toast.makeText(getContext(), R.string.no_internet_connection_please_connect_to_the_internet_and_try_again, Toast.LENGTH_LONG).show();
            }
        }
    }

    void getTimeLine() {
        Intent timeLineIntent = new Intent(Intent.ACTION_SYNC, null, getActivity(), UserTimelineService.class);
        timeLineIntent.putExtra(Constants.USER_ID_EXTRA, Long.parseLong(mFollower.getId()));
        timeLineIntent.putExtra(Constants.PAGESIZE_EXTRA, PAGESIZE);

        getContext().startService(timeLineIntent);


    }

    private void populateTweets() {
        StikkyHeaderBuilder.stickTo(mScrollView)
                .setHeader(R.id.header, containerLinearLayout)
                .minHeightHeader(100)
                .animator(new IconAnimator())
                .build();
        mScrollView.setPadding(0, 0, 0, 0);
        for (int i = 0; i < tweets.length; i++) {
            tweetsLinearLayout.addView(
                    new TweetView(getContext(), tweets[i])
            );
        }
        if (tweets.length == 0) {
            Toast.makeText(getContext(), R.string.sorry_there_are_no_tweets, Toast.LENGTH_SHORT).show();
        }
    }

    public class FollowerInfoBroadcastReceiver extends BroadcastReceiver {

        public static final String PROCESS_USER_INFO_RESPONSE = "twittertest.bassem.com.twittertest.intent.action.PROCESS_USER_INFO_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String body = intent.getStringExtra(Constants.RESULT_EXTRA);
                tweets = GsonHelper.parseUserTimeLineResponse(body);
                populateTweets();
            }
        }


    }

    @Override
    public void onDestroy() {
        if (mReceiver != null)
            getContext().unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
