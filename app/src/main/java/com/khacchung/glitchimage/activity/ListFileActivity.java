package com.khacchung.glitchimage.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentTransaction;

import com.facebook.ads.AbstractAdListener;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.khacchung.glitchimage.R;
import com.khacchung.glitchimage.application.MyApplication;
import com.khacchung.glitchimage.base.BaseActivity;
import com.khacchung.glitchimage.customs.CallBackClick;
import com.khacchung.glitchimage.customs.CallBackPermission;
import com.khacchung.glitchimage.customs.EditListenner;
import com.khacchung.glitchimage.customs.UpdateList;
import com.khacchung.glitchimage.fragment.ImageCreatedFragment;
import com.khacchung.glitchimage.fragment.ListFileFragment;
import com.khacchung.glitchimage.fragment.VideoCreatedFragment;
import com.khacchung.glitchimage.util.AdsUtil;
import com.khacchung.glitchimage.util.AudienceNetworkInitializeHelper;
import com.khacchung.glitchimage.util.PathManager;

import java.io.File;
import java.util.ArrayList;

public class ListFileActivity extends BaseActivity implements CallBackClick, UpdateList, EditListenner {
    public static final String TYPE = "TYPE";
    public static final String PATH = "PATH";
    public static final int TYPE_IMG = 1;
    public static final int TYPE_VIDEO = 2;
    private static final String TAG = ListFileActivity.class.getSimpleName();

    private ImageCreatedFragment imageCreatedFragment;
    private VideoCreatedFragment videoCreatedFragment;

    private ListFileFragment listFileFragment;

    private ArrayList<String> listImages = new ArrayList<>();
    private ArrayList<String> listVideos = new ArrayList<>();
    public static boolean isVideo = false;
    private AdView adView;
    private com.google.android.gms.ads.AdView mAdView;
    private AdRequest adRequest;

    public static void startIntent(BaseActivity activity, String path, int type) {
        Intent intent = new Intent(activity, ListFileActivity.class);
        intent.putExtra(PATH, path);
        intent.putExtra(TYPE, type);
        activity.startActivity(intent);
        MyApplication.addCountAction();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        hidenStatusBar();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_file);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
        }
        enableBackButton();
        Intent intent = getIntent();
        String path = intent.getStringExtra(PATH);
        int type = intent.getIntExtra(TYPE, 0);
        if (path != null && type == TYPE_VIDEO) {
            isVideo = true;
        } else if (path != null && type == TYPE_IMG) {
            isVideo = false;
        }
        //ads
        AudienceNetworkInitializeHelper.initialize(this);

        checkPermission(new String[]{
                BaseActivity.PER_READ,
                BaseActivity.PER_WRITE
        }, new CallBackPermission() {
            @Override
            public void grantedFullPermission() {
                initView();
            }

            @Override
            public void notFullPermission() {
                Toast.makeText(ListFileActivity.this,
                        getResources().getString(R.string.error_permission),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mInterstitialAd.loadAd(new AdRequest.Builder()
                .addTestDevice(AdsUtil.HASHED_ID)
                .build());
    }

    private void initView() {
        //ads
        adView = new AdView(this, AdsUtil.BANNER_ID, AdSize.BANNER_HEIGHT_50);
        mAdView = findViewById(R.id.adView);
        // Find the Ad Container
        LinearLayout adContainer = findViewById(R.id.banner_container);
        // Add the ad view to your activity layout
        adContainer.addView(adView);
        // Request an ad
        adView.loadAd();
        adView.setAdListener(new AbstractAdListener() {
            @Override
            public void onError(Ad ad, AdError error) {
                super.onError(ad, error);
                Log.e(TAG, "loadAdsFacebook onError()");
                adRequest = new AdRequest.Builder()
                        .addTestDevice(AdsUtil.HASHED_ID)
                        .build();
                mAdView.loadAd(adRequest);
            }
        });

        listImages = new ArrayList<>();
        listVideos = new ArrayList<>();

        imageCreatedFragment = new ImageCreatedFragment(this, listImages, this, myApplication, this);
        videoCreatedFragment = new VideoCreatedFragment(this, listVideos, this);

        listFileFragment = new ListFileFragment(this, this, imageCreatedFragment, videoCreatedFragment);

        addFragment();

        getAllImagesIsCreated();
        getAllVideosIsCreated();

        cancleLoading();
    }

    private void addFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_render, listFileFragment);
        transaction.commit();
    }

    @Override
    public void ClickImage(int pos) {
        if (pos >= 0 && pos < listImages.size()) {
            if (isReadyShowInAds()) {
                interstitialAd.loadAd();
                interstitialAd.setAdListener(new AbstractAdListener() {
                    @Override
                    public void onError(Ad ad, AdError error) {
                        super.onError(ad, error);
                        Log.e(TAG, "load Ads Facebook onError()");
                        mInterstitialAd.setAdListener(new AdListener() {
                            @Override
                            public void onAdClosed() {
                                super.onAdClosed();
                                PreviewActivity.startIntent(ListFileActivity.this,
                                        listImages.get(pos), ListFileActivity.TYPE_IMG);
                            }

                            @Override
                            public void onAdFailedToLoad(int i) {
                                super.onAdFailedToLoad(i);
                                Log.e(TAG, "load Admob onError()");
                                PreviewActivity.startIntent(ListFileActivity.this,
                                        listImages.get(pos), ListFileActivity.TYPE_IMG);
                            }
                        });
                        if (mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                        } else {
                            PreviewActivity.startIntent(ListFileActivity.this,
                                    listImages.get(pos), ListFileActivity.TYPE_IMG);
                        }
                    }

                    @Override
                    public void onAdLoaded(Ad ad) {
                        super.onAdLoaded(ad);
                        interstitialAd.show();
                    }

                    @Override
                    public void onInterstitialDismissed(Ad ad) {
                        super.onInterstitialDismissed(ad);
                        PreviewActivity.startIntent(ListFileActivity.this,
                                listImages.get(pos), ListFileActivity.TYPE_IMG);
                    }
                });
            } else {
                PreviewActivity.startIntent(this, listImages.get(pos), ListFileActivity.TYPE_IMG);
            }
        }
    }

    @Override
    public void ClickVideo(int pos) {
        if (pos >= 0 && pos < listVideos.size()) {
            if (isReadyShowInAds()) {
                interstitialAd.loadAd();
                interstitialAd.setAdListener(new AbstractAdListener() {
                    @Override
                    public void onError(Ad ad, AdError error) {
                        super.onError(ad, error);
                        Log.e(TAG, "load Ads Facebook onError()");
                        mInterstitialAd.setAdListener(new AdListener() {
                            @Override
                            public void onAdClosed() {
                                super.onAdClosed();
                                PreviewActivity.startIntent(ListFileActivity.this,
                                        listVideos.get(pos), ListFileActivity.TYPE_VIDEO);
                            }

                            @Override
                            public void onAdFailedToLoad(int i) {
                                super.onAdFailedToLoad(i);
                                Log.e(TAG, "load Admob onError()");
                                PreviewActivity.startIntent(ListFileActivity.this,
                                        listVideos.get(pos), ListFileActivity.TYPE_VIDEO);
                            }
                        });
                        if (mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                        } else {
                            PreviewActivity.startIntent(ListFileActivity.this,
                                    listVideos.get(pos), ListFileActivity.TYPE_VIDEO);
                        }
                    }

                    @Override
                    public void onAdLoaded(Ad ad) {
                        super.onAdLoaded(ad);
                        interstitialAd.show();
                    }

                    @Override
                    public void onInterstitialDismissed(Ad ad) {
                        super.onInterstitialDismissed(ad);
                        PreviewActivity.startIntent(ListFileActivity.this,
                                listVideos.get(pos), ListFileActivity.TYPE_VIDEO);
                    }
                });
            } else
                PreviewActivity.startIntent(this, listVideos.get(pos), ListFileActivity.TYPE_VIDEO);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void getAllVideosIsCreated() {
        listVideos.clear();
        String parentPath = PathManager.getPathFolder(this) +
                PathManager.FOLDER_VIDEO;
        File file3 = new File(parentPath);
        if (!file3.exists()) {
            file3.mkdirs();
        }

        File[] file = new File(parentPath).listFiles();
        if (file != null)
            if (file.length > 0)
                for (File f : file) {
                    listVideos.add(f.getAbsolutePath());
                    Log.e(TAG, "getAllVideosIsCreated(): " + f.getAbsolutePath());
                }

        if (videoCreatedFragment != null) {
            videoCreatedFragment.updateListVideo(listVideos);
        }
    }

    private void getAllImagesIsCreated() {
        listImages.clear();
        String parentPath = PathManager.getPathFolder(this) +
                PathManager.FOLDER_IMAGE;
        File file2 = new File(parentPath);
        if (!file2.exists()) {
            file2.mkdirs();
        }
        File[] listFile = new File(parentPath).listFiles();
        if (listFile != null)
            if (listFile.length > 0) {
                for (File file : listFile) {
                    if (!file.isDirectory()) {
                        if (file.getName().endsWith(".png")
                                || file.getName().endsWith(".jpg")
                                || file.getName().endsWith(".jpeg")) {
                            if (!listImages.contains(file.getAbsolutePath()))
                                listImages.add(file.getAbsolutePath());
                            Log.e(TAG, "getAllImagesIsCreated(): " + file.getAbsolutePath());
                        }
                    }
                }
            }
        if (imageCreatedFragment != null) {
            imageCreatedFragment.updateListImage(listImages);
        }
    }

    @Override
    public void listIsChange(ArrayList<String> list) {
        this.listImages = list;
        if (imageCreatedFragment != null) {
            imageCreatedFragment.updateListImage(list);
        }
    }

    @Override
    public void editImageAtPos(int pos) {
        if (isReadyShowInAds()) {
            interstitialAd.loadAd();
            interstitialAd.setAdListener(new AbstractAdListener() {
                @Override
                public void onError(Ad ad, AdError error) {
                    super.onError(ad, error);
                    Log.e(TAG, "load Ads Facebook onError()");
                    mInterstitialAd.setAdListener(new AdListener() {
                        @Override
                        public void onAdClosed() {
                            super.onAdClosed();
                            runOnUiThread(() -> gotoGlitchImage(listImages.get(pos)));
                        }

                        @Override
                        public void onAdFailedToLoad(int i) {
                            super.onAdFailedToLoad(i);
                            Log.e(TAG, "load Admob onError()");
                            runOnUiThread(() -> gotoGlitchImage(listImages.get(pos)));
                        }
                    });
                    if (mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                    } else {
                        runOnUiThread(() -> gotoGlitchImage(listImages.get(pos)));
                    }
                }

                @Override
                public void onAdLoaded(Ad ad) {
                    super.onAdLoaded(ad);
                    interstitialAd.show();
                }

                @Override
                public void onInterstitialDismissed(Ad ad) {
                    super.onInterstitialDismissed(ad);
                    runOnUiThread(() -> gotoGlitchImage(listImages.get(pos)));
                }
            });
        } else {
            runOnUiThread(() -> gotoGlitchImage(listImages.get(pos)));
        }
    }

    @Override
    public void editVideoAtPos(int pos) {

    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }
}
