package com.adssetup.adssdk.config;

public class AdsConfig {
    private boolean appAllAdsShow = true;
    private String adPriority = "google";
    private boolean googleAdFailedShowQuiz = true;
    private final MainScreen mainScreen = new MainScreen();
    private final SecondScreen secondScreen = new SecondScreen();

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

    public MainScreen getMainScreen() {
        return mainScreen;
    }

    public SecondScreen getSecondScreen() {
        return secondScreen;
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
    }
}