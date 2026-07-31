package com.qihe.clipflow.ui.xiaohongshu

import android.app.Application
import com.qihe.clipflow.data.repository.SupportedPlatform
import com.qihe.clipflow.ui.parser.PlatformParseViewModel

class XiaohongshuViewModel(application: Application) :
    PlatformParseViewModel(application, SupportedPlatform.XIAOHONGSHU)
