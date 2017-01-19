package com.laifeng.sopcastsdk.blacklist;

import android.os.Build;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @Title: BlackListHelper
 * @Package com.laifeng.sopcastsdk.blacklist
 * @Description:
 * @Author Jim
 * @Date 2016/10/27
 * @Time 上午10:40
 * @Version
 */

public class BlackListHelper {
    private static final String[] BLACKLISTED_AEC_MODELS = new String[] {
            "Nexus 5", // Nexus 5
    };

    private static final String[] BLACKLISTED_FPS_MODELS = new String[] {
            "OPPO R9",
            "Nexus 6P",
    };


    public static boolean deviceInAecBlacklisted() {
        List<String> blackListedModels = Arrays.asList(BLACKLISTED_AEC_MODELS);
        for(String blackModel : blackListedModels) {
            String model = Build.MODEL;
            if(!TextUtils.isEmpty(model) && model.contains(blackModel)) {
                return true;
            }
        }
        return false;
    }

    public static boolean deviceInFpsBlacklisted() {
        List<String> blackListedModels = Arrays.asList(BLACKLISTED_FPS_MODELS);
        for(String blackModel : blackListedModels) {
            String model = Build.MODEL;
            if(!TextUtils.isEmpty(model) && model.contains(blackModel)) {
                return true;
            }
        }
        return false;
    }
}
