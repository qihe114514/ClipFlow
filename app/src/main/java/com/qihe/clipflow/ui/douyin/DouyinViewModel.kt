package com.qihe.clipflow.ui.douyin

import android.app.Application
import com.qihe.clipflow.data.repository.SupportedPlatform
import com.qihe.clipflow.ui.parser.PlatformParseViewModel

class DouyinViewModel(application: Application) :
    PlatformParseViewModel(application, SupportedPlatform.DOUYIN)
