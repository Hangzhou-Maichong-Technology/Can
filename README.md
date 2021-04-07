# Can
## 迈冲科技 Can 库
杭州迈冲科技 Can 库通用实例。

## 一、创建 AndroidStudio 项目，导入库文件

将 mcCan.aar 文件拷贝到 libs 目录下。

在```app```目录中的```build.gradle```文件中添加如下依赖：

```groovy
    implementation files('libs/mcCan.aar')
```

## 二、使用接口

### 1，打开
初始化 CanUtil 进行参数配置后，通过 canOpen 方法打开 Can 总线。
```java
canUtil = new CanUtils(selectCan, String.valueOf(selectBaudRate));
canUtil.canOpen();
```

### 2，读取
开启线程读取数据。
```java
Executors.newCachedThreadPool().execute(new Runnable() {
    @Override
    public void run() {
        while (recvAlive.get()) {
            if (canUtil != null) {
                CanFrame recvFrame = canUtil.canReadBytes(1， false);
                if (recvFrame != null && recvFrame.data != null && recvFrame.data.length > 0) {
                    Log.i(TAG, "recv can == " + recvFrame.toString());
                }
            }

            SystemClock.sleep(100);
        }
    }
});
```

### 3，写入
构建 CanFrame 类封装数据后，写入 Can 总线。

```java
CanFrame canFrame = new CanFrame();
canFrame.canId = 0;
canFrame.idExtend = false;  // id 是否为扩展帧
canFrame.len = bytes.length;
canFrame.data = bytes;
canUtil.canWriteBytes(canFrame);
```

### 4，关闭
使用结束后需要关闭 Can 总线。
```java
canUtil.canClose();
```

## 三、下载体验
[can 实例 apk 下载](https://github.com/Hangzhou-Maichong-Technology/Can/raw/master/apk/Can.apk)