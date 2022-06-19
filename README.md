# AndroidDemo

## StripeProgressBarDemo - 条纹进度条（可自由定制）
### 1. 效果

https://user-images.githubusercontent.com/54576066/174481193-6cdcddc8-d8dc-42ec-84d5-3d95d1e21c9b.mp4

### 2. 定制条件
![PicDraft](https://user-images.githubusercontent.com/54576066/174487846-3b012bb0-a18b-4267-8a5d-b6be50b8c33a.png)

### 3. 代码实现
#### 目标：
* 单个 canvas 图层实现，且避免 canvas 的旋转平移
* 避免 onDraw 里 drawBitmap 或 clipPath
* 实现无限匀速平移的动画效果，避免跳跃或卡顿
#### 实现方案：
1. 通过一个有整个控件 2 倍长的 “条纹矩形” 的不断左移来实现无限平移动画。
2. “条纹” 通过 Path 画平行四边形来实现（注意条纹旋转角度过大时，要填充左右两边的 ”空白“ 部分，以实现平移时的无缝衔接）。
3. 控件初始化时，启动一个 infinite 执行的 ValueAnimator，利用 duration + ofFloat 控制速度，每当移动了整个控件 1 倍长时 repeat。不断更新上述 “条纹矩形” Bitmap 的左边坐标。
4. onLayout 时：
- （1）准备好上述 “条纹矩形” 的 Bitmap，将其设置为 Paint 的 BitmapShader。
- （2）onLayout 还要通过一个 Path 准备好 bar 的圆角矩形外轮廓。
5. onDraw 时：
- （1）先画出背景的圆角矩形。
- （2）然后根据此时的 progress 值画出响应长度的 Path 矩形，将其与 4.（2） 的 Path 进行 InterSect 处理。得到 bar 此时的最终外轮廓。
- （3）为 4.（1） 的 BitmapShader 设置 LocalMatrix，根据 3. 的左边坐标来设置横移位置。
- （4）最后 drawPath 画出 bar。
6. detach 窗口时，取消动画并及时释放 Bitmap 资源。
