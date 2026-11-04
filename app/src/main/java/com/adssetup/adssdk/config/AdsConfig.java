package com.adssetup.adssdk.config;

import java.util.ArrayList;
import java.util.List;

public class AdsConfig {
    private boolean appAllAdsShow = true;
    private String adPriority = "google";
    private boolean googleAdFailedShowQuiz = true;
    private String interstitialLoadType = "load";
    private String appOpenLoadType = "preload";
    private final MainScreen mainScreen = new MainScreen();
    private final SecondScreen secondScreen = new SecondScreen();
    private final QuizAdsDesign quizAdsDesign = new QuizAdsDesign();

    public boolean isAppAllAdsShow() {
        return appAllAdsShow;
    }

    public void setAppAllAdsShow(boolean appAllAdsShow) {
        this.appAllAdsShow = appAllAdsShow;
    }

    public String getAdPriority() {
        return adPriority;
    }

    public void setAdPriority(String adPriority) {
        this.adPriority = adPriority;
    }

    public boolean isGoogleAdFailedShowQuiz() {
        return googleAdFailedShowQuiz;
    }

    public void setGoogleAdFailedShowQuiz(boolean googleAdFailedShowQuiz) {
        this.googleAdFailedShowQuiz = googleAdFailedShowQuiz;
    }

    public String getInterstitialLoadType() {
        return interstitialLoadType;
    }

    public void setInterstitialLoadType(String interstitialLoadType) {
        this.interstitialLoadType = interstitialLoadType;
    }

    public String getAppOpenLoadType() {
        return appOpenLoadType;
    }

    public void setAppOpenLoadType(String appOpenLoadType) {
        this.appOpenLoadType = appOpenLoadType;
    }

    public MainScreen getMainScreen() {
        return mainScreen;
    }

    public SecondScreen getSecondScreen() {
        return secondScreen;
    }

    public QuizAdsDesign getQuizAdsDesign() {
        return quizAdsDesign;
    }

    public static class MainScreen {
        private boolean mainAdShow = true;
        private String mainAdType = "banner";
        private String mainBannerId = "ca-app-pub-3940256099942544/9214589741";
        private String mainNativeId = "ca-app-pub-3940256099942544/2247696110";
        private boolean mainButtonAdShow = true;
        private String mainButtonAdType = "inter";
        private String mainInterstitialId = "ca-app-pub-3940256099942544/1033173712";
        private String mainAppOpenId = "ca-app-pub-3940256099942544/9257395921";
        private boolean mainPipAdShow = true;
        private String mainPipId = "ca-app-pub-3940256099942544/9657123429";

        public boolean isMainAdShow() {
            return mainAdShow;
        }

        public void setMainAdShow(boolean mainAdShow) {
            this.mainAdShow = mainAdShow;
        }

        public String getMainAdType() {
            return mainAdType;
        }

        public void setMainAdType(String mainAdType) {
            this.mainAdType = mainAdType;
        }

        public String getMainBannerId() {
            return mainBannerId;
        }

        public void setMainBannerId(String mainBannerId) {
            this.mainBannerId = mainBannerId;
        }

        public String getMainNativeId() {
            return mainNativeId;
        }

        public void setMainNativeId(String mainNativeId) {
            this.mainNativeId = mainNativeId;
        }

        public boolean isMainButtonAdShow() {
            return mainButtonAdShow;
        }

        public void setMainButtonAdShow(boolean mainButtonAdShow) {
            this.mainButtonAdShow = mainButtonAdShow;
        }

        public String getMainButtonAdType() {
            return mainButtonAdType;
        }

        public void setMainButtonAdType(String mainButtonAdType) {
            this.mainButtonAdType = mainButtonAdType;
        }

        public String getMainInterstitialId() {
            return mainInterstitialId;
        }

        public void setMainInterstitialId(String mainInterstitialId) {
            this.mainInterstitialId = mainInterstitialId;
        }

        public String getMainAppOpenId() {
            return mainAppOpenId;
        }

        public void setMainAppOpenId(String mainAppOpenId) {
            this.mainAppOpenId = mainAppOpenId;
        }

        public boolean isMainPipAdShow() {
            return mainPipAdShow;
        }

        public void setMainPipAdShow(boolean mainPipAdShow) {
            this.mainPipAdShow = mainPipAdShow;
        }

        public String getMainPipId() {
            return mainPipId;
        }

        public void setMainPipId(String mainPipId) {
            this.mainPipId = mainPipId;
        }
    }

    public static class SecondScreen {
        private boolean secondAdShow = true;
        private String secondAdType = "banner";
        private String secondBannerId = "ca-app-pub-3940256099942544/9214589741";
        private String secondNativeId = "ca-app-pub-3940256099942544/2247696110";
        private boolean secondButtonAdShow = true;
        private String secondButtonAdType = "inter";
        private String secondInterstitialId = "ca-app-pub-3940256099942544/1033173712";
        private String secondAppOpenId = "ca-app-pub-3940256099942544/9257395921";
        private boolean secondRewardAdShow = true;
        private String secondRewardId = "ca-app-pub-3940256099942544/5224354917";

        public boolean isSecondAdShow() {
            return secondAdShow;
        }

        public void setSecondAdShow(boolean secondAdShow) {
            this.secondAdShow = secondAdShow;
        }

        public String getSecondAdType() {
            return secondAdType;
        }

        public void setSecondAdType(String secondAdType) {
            this.secondAdType = secondAdType;
        }

        public String getSecondBannerId() {
            return secondBannerId;
        }

        public void setSecondBannerId(String secondBannerId) {
            this.secondBannerId = secondBannerId;
        }

        public String getSecondNativeId() {
            return secondNativeId;
        }

        public void setSecondNativeId(String secondNativeId) {
            this.secondNativeId = secondNativeId;
        }

        public boolean isSecondButtonAdShow() {
            return secondButtonAdShow;
        }

        public void setSecondButtonAdShow(boolean secondButtonAdShow) {
            this.secondButtonAdShow = secondButtonAdShow;
        }

        public String getSecondButtonAdType() {
            return secondButtonAdType;
        }

        public void setSecondButtonAdType(String secondButtonAdType) {
            this.secondButtonAdType = secondButtonAdType;
        }

        public String getSecondInterstitialId() {
            return secondInterstitialId;
        }

        public void setSecondInterstitialId(String secondInterstitialId) {
            this.secondInterstitialId = secondInterstitialId;
        }

        public String getSecondAppOpenId() {
            return secondAppOpenId;
        }

        public void setSecondAppOpenId(String secondAppOpenId) {
            this.secondAppOpenId = secondAppOpenId;
        }

        public boolean isSecondRewardAdShow() {
            return secondRewardAdShow;
        }

        public void setSecondRewardAdShow(boolean secondRewardAdShow) {
            this.secondRewardAdShow = secondRewardAdShow;
        }

        public String getSecondRewardId() {
            return secondRewardId;
        }

        public void setSecondRewardId(String secondRewardId) {
            this.secondRewardId = secondRewardId;
        }
    }

    public static class QuizAdsDesign {
        private List<String> quizAppIcon = new ArrayList<>();
        private List<String> quizNativeMedia = new ArrayList<>();
        private List<String> quizInterstitialMedia = new ArrayList<>();
        private List<String> quizAppOpenMedia = new ArrayList<>();
        private List<String> quizTitle = new ArrayList<>();
        private List<String> quizDescription = new ArrayList<>();
        private String quizButtonText = "";

        public List<String> getQuizAppIcon() {
            return quizAppIcon;
        }

        public void setQuizAppIcon(List<String> quizAppIcon) {
            this.quizAppIcon = quizAppIcon;
        }

        public List<String> getQuizNativeMedia() {
            return quizNativeMedia;
        }

        public void setQuizNativeMedia(List<String> quizNativeMedia) {
            this.quizNativeMedia = quizNativeMedia;
        }

        public List<String> getQuizInterstitialMedia() {
            return quizInterstitialMedia;
        }

        public void setQuizInterstitialMedia(List<String> quizInterstitialMedia) {
            this.quizInterstitialMedia = quizInterstitialMedia;
        }

        public List<String> getQuizAppOpenMedia() {
            return quizAppOpenMedia;
        }

        public void setQuizAppOpenMedia(List<String> quizAppOpenMedia) {
            this.quizAppOpenMedia = quizAppOpenMedia;
        }

        public List<String> getQuizTitle() {
            return quizTitle;
        }

        public void setQuizTitle(List<String> quizTitle) {
            this.quizTitle = quizTitle;
        }

        public List<String> getQuizDescription() {
            return quizDescription;
        }

        public void setQuizDescription(List<String> quizDescription) {
            this.quizDescription = quizDescription;
        }

        public String getQuizButtonText() {
            return quizButtonText;
        }

        public void setQuizButtonText(String quizButtonText) {
            this.quizButtonText = quizButtonText;
        }
    }
}