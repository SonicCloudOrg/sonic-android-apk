<p align="center">
  <img src="https://raw.githubusercontent.com/SonicCloudOrg/sonic-server/main/logo.png">
</p>
<p align="center">🎉Android Plugin of Sonic cloud real machine testing platform.</p>

 <a href="#">  
    <img src="https://img.shields.io/github/downloads/SonicCloudOrg/sonic-android-apk/total">
  </a>

## 背景与灵感
> 当前sonic-android-apk仓库灵感来源于[STFService.apk](https://github.com/DeviceFarmer/STFService.apk)，对其部分代码进行扩展和去除不需要的模块。
>
> 非常感谢STF和Scrcpy提供的思路！Sonic后续会建设更多不一样的新功能，以便于跟Sonic更好的交互。

### 基于STFService.apk改造功能
> 1. 改造物理查找设备，替换背景颜色与细节优化
> 2. 改造触控Service实时触控，增加流畅性

### Sonic扩展功能
> 1. 获取app列表，包括app图标
> 2. 远程音频采集
> 3. PCM格式音频进行ACC转码
> 4. 横竖屏实时监听
> 5. 剪切板管理优化（doing）
> 6. 网络抓包（doing）
> 7. 一键连接wifi（doing）
> 8. 其他扩展

### Build
```
gradlew assembleDebug
```

### 开源协议

> 当前sonic-android-apk仓库已遵循STF的Apache License，详见[LICENSE](LICENSE)
