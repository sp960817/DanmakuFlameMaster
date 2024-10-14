DanmakuFlameMaster(Optimize)
==================

[原始仓库和文档](https://github.com/bilibili/DanmakuFlameMaster)

### 仓库背景
&emsp;&emsp;之前在熊猫直播(已破产😢)工作，了解过一些弹幕库的内容(熊猫直播用的就是B站的开源弹幕库)，当时有点兴趣就看源码，想着能不能优化一下(B站好像不维护了),所以有了这个库。（这个仓库代码没有在线上验证过，仅当学习参考）
### 修改点
  - **去除CacheManagingDrawTask清除弹幕缓存等待30毫秒**<br/>
      原因:在弹幕量比较大时，弹幕移除屏幕外需要释放cache(Bitmap),清除每一cache会等待30毫秒，导致Cache线程不能快速构建没有显示的cache，从而导致UI线程直接绘制,造成卡顿.
  	```groovy
		synchronized (mDrawingNotify) {        			
            try {
                mDrawingNotify.wait(30);
         	} catch (InterruptedException e) {
                e.printStackTrace();
                return ACTION_BREAK;
            }
		}
	```
  - **强制在子线程中构建cache**<br/>
    &emsp;&emsp;弹幕库默认的实现是:当cache线程在UI线程需要绘制某一个弹幕时还没有准备好对应的cache，则会在UI线程构建cache并绘制到弹幕View上直接绘制是一个比较耗时的操作(可能需要1-10ms),这样的弹幕越多，造成主线程的卡顿越明显.<br/>
    &emsp;&emsp;此修改可以选择强制在弹幕计算需要绘制的弹幕(子线程)时判断cache状态，如果没有则直接在改线程触发构建cache,构建完成后才会将该弹幕交给UI线程渲染。<br/>
    &emsp;&emsp;此方案有一个弊端，就是当弹幕密度非常大时会造成弹幕拥堵，方案会自动将弹幕延期绘制，保证连续性.<br/>
    &emsp;&emsp;延期绘制是可选择性开启，默认关闭.
    
  - **替换canvas绘制到OpenGL绘制**<br/>
    &emsp;&emsp;弹幕库使用cache线程计算cache(Bitmap)，UI线程使用canvas绘制bitmap实现，虽然绘制bitmap非常快，但有两点依然存在的弊端.<br/>
     - 依然需要耗费UI线程的计算力，密度大时即使全部命中cache，也可能造成卡顿.
     - cache将在整个弹幕可见期间完全处于内存中，造成JVM内存压力大(粗略计算50条/s时内存占用在150MB+)。

- 使用多种方式(View/SurfaceView/TextureView)实现高效绘制

- B站xml弹幕格式解析

- 基础弹幕精确还原绘制

- 支持mode7特殊弹幕

- 多核机型优化，高效的预缓存机制

- 支持多种显示效果选项实时切换

- 实时弹幕显示支持

- 换行弹幕支持/运动弹幕支持

- 支持自定义字体

- 支持多种弹幕参数设置

- 支持多种方式的弹幕屏蔽

### TODO:

- 增加OpenGL ES绘制方式


### Download
Download the [latest version][1] or grab via Maven:

```groovy
repositories {
    maven("https://jitpack.io") {
        content {
            includeVersionByRegex("com.github.*", ".*", ".*")
        }
    }
}

dependencies {
    compile 'com.github.fengymi.DanmakuFlameMaster:DanmakuFlameMaster:1.0.1'
    compile 'com.github.fengymi.DanmakuFlameMaster:ndkbitmap-armv7a:1.0.1'

    # Other ABIs: optional
    compile 'com.github.fengymi.DanmakuFlameMaster:ndkbitmap-armv5:1.0.1'
    compile 'com.github.fengymi.DanmakuFlameMaster:ndkbitmap-x86:1.0.1'
}
```
Snapshots of the development version are available in [Sonatype's snapshots repository][2].


### License
    Copyright (C) 2013-2015 Chen Hui <calmer91@gmail.com>
    Licensed under the Apache License, Version 2.0 (the "License");

