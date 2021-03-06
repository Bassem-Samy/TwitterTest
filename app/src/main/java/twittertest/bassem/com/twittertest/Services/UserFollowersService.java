package twittertest.bassem.com.twittertest.Services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.JsonElement;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import twittertest.bassem.com.twittertest.Models.Follower;
import twittertest.bassem.com.twittertest.Models.GetUserFollowersResponse;
import twittertest.bassem.com.twittertest.fragments.FragmentUserFollowers;
import twittertest.bassem.com.twittertest.helpers.Constants;
import twittertest.bassem.com.twittertest.helpers.DatabaseHelper;
import twittertest.bassem.com.twittertest.helpers.GsonHelper;
import twittertest.bassem.com.twittertest.helpers.MyUtilities;
import twittertest.bassem.com.twittertest.helpers.TwitterHelper;

/**
 * Created by Bassem Samy on 10/21/2016.
 */

public class UserFollowersService extends IntentService {

    private long userId;
    private String cursor;
    private int pageSize;
    private int totalItemsSize;
    private DatabaseHelper dbHelper;

    public UserFollowersService() {
        super(UserFollowersService.class.getName());
        dbHelper = new DatabaseHelper(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            userId = intent.getLongExtra(Constants.USER_ID_EXTRA, 0);
            cursor = intent.getStringExtra(Constants.CURSOR_EXTRA);
            pageSize = intent.getIntExtra(Constants.PAGESIZE_EXTRA, 0);
            totalItemsSize = intent.getIntExtra(Constants.CURRENTUSERSCOUNT_EXTRA, 0);
            try {
                if (MyUtilities.checkForInternet(this)) {
                    retrofit2.Response<JsonElement> res = TwitterHelper.GetFollowers(userId, pageSize, cursor);
                    if (res.isSuccessful()) {
                        GetUserFollowersResponse response = GsonHelper.parseUserFollowersResponse(res.body());
                        updateDatabase(response);

                    } else {
                        sendBackBroadCast(null);

                    }
                } else {
                    loadOffline();
                }
            } catch (Exception ex) {
                GetUserFollowersResponse res = new GetUserFollowersResponse();
                sendBackBroadCast(res);


            }
        }
    }

    private void loadOffline() {
        GetUserFollowersResponse response = new GetUserFollowersResponse();

        try {
            final Dao<Follower, Integer> followersDao = dbHelper.getFollowerDao();
            int totalNumber = (int) followersDao.countOf();
            QueryBuilder<Follower, Integer> builder = followersDao.queryBuilder();
            builder.offset((long) totalItemsSize).limit((long) pageSize);
            response.setFollowers((ArrayList<Follower>) followersDao.query(builder.prepare()));
            if (response.getFollowers().size() + totalItemsSize == totalNumber)
                response.setNext_cursor("0");
            else
                response.setNext_cursor("-1");
        } catch (Exception ex) {
        }
        sendBackBroadCast(response);

    }

    private void updateDatabase(final GetUserFollowersResponse response) {
        try {
            final Dao<Follower, Integer> followersDao = dbHelper.getFollowerDao();
            followersDao.callBatchTasks(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    if (response != null && response.getFollowers() != null) {
                        for (Follower f : response.getFollowers())
                            followersDao.createOrUpdate(f);
                    }
                    sendBackBroadCast(response);
                    return null;
                }
            });


        } catch (Exception ex) {
            Log.e("save followers", ex.toString());
        }
    }

    private void sendBackBroadCast(GetUserFollowersResponse response) {

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(FragmentUserFollowers.GetUserFollowersReceiver.PROCESS_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.RESULT_EXTRA, response);
        broadcastIntent.putExtras(bundle);
        sendBroadcast(broadcastIntent);
    }
}
