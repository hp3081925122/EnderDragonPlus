# Ender Dragon Plus 1.21.1 NeoForge → 1.20.1 Forge 迁移

## 基线与边界
- 源目录：`EnderDragonPlus-1211-neoforge`，源分支 `main`，提交 `f454685` 加迁移开始时未提交的正式战斗代码。
- 目标：独立 Git 工作树 `1.20.1-forge-EnderDragonPlus`，分支 `1201forge`。
- 环境：Minecraft 1.20.1、Forge 47.4.23、Java 17、官方映射、Gradle 8.8。
- 原 1.21.1 项目没有切分支、提交或修改源码。
- 按用户补充要求，不迁移 Boss 展示命令、刷靶怪、自动放技能、运镜、字幕、录屏、展示免伤、展示选敌和专用消息。目标源码与正式 JAR 均没有这些类、命令和分支。

## 功能完整性
保留全部 12 个技能、13 个自定义龙阶段、6 种具体持续危害、阶段血量百分比、攻击属性乘技能倍率、击退抗性、全部配置键/默认值/范围、阶段存档、特殊水晶召唤、护盾复活与引爆水晶、回复上限、配方和物品闪光。保留战斗消息 0–16，仅删除展示消息 17。

技能清单：虚空吐息、末影陨星、空间裂隙冲刺、虚空地脉、翼刃风暴、黑洞、龙鳞弹幕、末影雷暴、裂空环波、终焉新星、虚空长枪、陨灭俯冲。转阶段护盾独立保留。

迁移后已做两轮只读源码审计，未发现用户排除范围以外的技能或特效删减。所有 GLSL 文件、龙纹理、物品模型、中英文语言资源保留。JSON 着色器仅增加旧版需要的 attributes，算法和 uniform 不变。

## 版本适配
- NeoForge 注册、配置及事件替换为 Forge；tick 使用 END 阶段，GUI 使用 Forge overlay。
- SimpleChannel 替换 CustomPacketPayload/StreamCodec；保留消息编号、NBT 字段、主线程处理、追踪同步和 320 格广播范围。
- Java 17 与完整 Forge wrapper/build；Mixin 保留全部正式注入并生成、打包 refmap，正式 JAR 执行 reobfJar。
- 顶点缓冲转换为 1.20.1 的 begin/endVertex/endOrDiscardIfEmpty，保留全部几何和法线。
- 1.20.1 的 ShaderInstance.apply 会应用旧版 BlendMode，故世界特效在 super.apply 之后恢复原来的加色或普通透明混合；屏幕效果仍独立处理。
- 为五个着色器 JSON 补充与 VertexFormat 对应的 attributes，恢复 1.21.1 自动完成的属性绑定。
- 龙模型身体/眼睛注入按 1.20.1 的方法重载与调用顺序定位；旧模型忽略 RGB 参数，使用 TintedVertexConsumer 保留阶段染色、裂纹与狂暴血管颜色，alpha、UV、光照、法线和几何全部转发。
- ClipContext 的空碰撞上下文使用旧构造函数的 null Entity；爆炸音效改为旧版 SoundEvent 字段；水晶随机打乱使用同一 RandomSource 的旧版 shuffle 入口。
- `recipe` 改为 `recipes`，配方结果 `id` 改为 `item`，pack_format=15；配置与描述均以 UTF-8 处理。

## 验证结果（2026-10-16）
### 构建与 JAR
最终 `clean build` 与后续元数据修正后的 `build` 均成功，包含 reobfJar。无 Java 编译错误；旧版 Forge API 有 4 条弃用警告。
正式 JAR 已核对 Forge 元数据、中文描述、MixinConfigs、5 个 Mixin 的 refmap 和配方；确认没有展示、录屏或临时验证类。

### 专用服务端
独立专用服务端、独立测试世界中共 **81 项断言通过**，结果见 [server-results.txt](validation/1201forge/server-results.txt)。覆盖：
- 全部自定义阶段注册、配置生命与攻击属性、配方载入、展示命令缺席。
- 普通/特殊召唤资格、17 类消息的完整 NBT 编解码与服务端发送。
- 攻击倍率、头部与翅膀接触伤害 Mixin、推拉及翅膀击退抗性。
- 12 个技能阶段完整执行；6 种具体危害的伤害与生命周期。
- 49%/24% 生命触发阶段、护盾与终焉新星；技能中保存安全阶段及读档恢复。
- 水晶回复上限、低于上限回复、关闭上限配置的原版回复。
- 终焉水晶放置、消耗、标记及物品闪光。
- 末地十柱水晶复活、保护/光束、按配置比例引爆、幸存水晶解除保护。
- 特殊水晶重生路径在入世界前标记强化龙并应用配置生命。

测试在真实服务端 Level/Entity 上使用专用 FakePlayer 与受控逐 tick 调用，包含实际伤害和存档断言；不代表人工完整打过一场战斗或长期多人联机压力测试。服务端测试快照的全部 src 文件与最终项目逐字节核对一致。测试脚本和测试模组均留在 `D:\AI临时文件\edp1201`，不进入正式源码或 JAR。

### Render Preview 两版对照
按要求使用 `E:/模组/发cf的模组/Render Preview` 的 1.20.1 Forge 与 1.21.1 NeoForge 代码，分别运行独立离线预览。固定相机、时间、输入数据，覆盖 17 类战斗消息在 20/70/120 tick 的样本、三个龙体阶段、屏幕特效和终焉水晶，共 **每版 56 个样本，112 次渲染成功，无预览渲染异常**。

对照结果见 [render-comparison.json](validation/1201forge/render-comparison.json)，逐版渲染记录也在同目录。8 对截图逐像素相同；不含原版龙模型/物品绘制的技能样本，最大全图 RGB 通道平均误差小于 0.000694（通道范围 0–255）。几何、混合、畸变与颜色已保留。修复了实测发现的龙体发光变白问题。

原版实体光照和物品附魔反光在两个 Minecraft 版本之间仍有差异，因此不声明全部截图逐像素相同；未用改动原版光照或删除效果掩盖差异。截图和完整运行日志留在 `D:\AI临时文件\edp1201`，记录中的 image 路径可定位原图。

## 产物
- `build/libs/enderdragonplus-1.20.1-forge-0.0.1.jar`
- SHA-256：`05d89e6d0f58f4bc5b83cc1eb9865bd88ea49b1caef8074856508a2b4131a495`
- 仅完成本地迁移和验证，没有上传 CurseForge 或推送远程。

## 实际战斗无特效修复与回归（2026-10-16）
此前离线预览没有覆盖 Forge 的真实世界事件参数，实际战斗暴露了矩阵适配错误。Forge 1.20.1 的 GameRenderer 在 AFTER_LEVEL 中传入局部投影栈，原实现误将该矩阵乘入 ModelView，导致自定义技能特效再次投影并被裁剪或错位。不是技能或贴图缺失，也不是已完成真实联机全量验收。

现在在 AFTER_WEATHER 保存正确的相机视图矩阵，仍在 AFTER_LEVEL 的原有顺序绘制，保留完整场景拷贝与畸变；切换世界时清空快照有效状态。增加受 debugLogging 配置控制的英文首帧渲染诊断。实际日志显示 capturedViewM33=1.0、eventPoseM33=0.0、shaderReady=true。

本次在用户世界的独立副本中启动真实 Minecraft 客户端与内置服务端，使用临时验证工具触发技能，依靠正常实体 tick、网络传输、客户端事件及世界渲染完成回归。12 个技能加转阶段护盾共 13 项运行完成，客户端实收战斗消息 0–16 全部类型，运行日志无 ERROR。逐项结果与关键日志见 [combat-results.txt](validation/1201forge/combat-results.txt)、[combat-log-excerpt.txt](validation/1201forge/combat-log-excerpt.txt)，截图汇总见 [combat-contact.png](validation/1201forge/combat-contact.png)。该轮确认了世界特效重新绘制，但错误地遗漏了截图中的黑白遮罩，不能据此称屏幕效果验收通过。黑白遮罩已在下节单独定位与修复。

该回归是实际单人游戏的客户端/内置服务端链路，不是外部专用服务端联机或多人压力测试。未改动用户原世界。测试类、自动触发、自动相机与截图代码仅在 D:/AI临时文件/edp1201-combat 临时目录中，干净重建的正式 JAR 已核对不包含这些类。旧版 JAR 在同一临时目录备份。

## 二阶段爆炸后黑白遮罩修复（2026-10-16）
用户指出的黑心白边画面是异常的原版暗角贴图覆盖，不是正常爆炸效果。之前实战截图出现过同样问题，阶段执行与网络收包成功没有证明 GUI 合成正确。

根因：Forge 1.20.1 的 ScreenFx 注册在原版 VIGNETTE 前。屏幕 shader 未声明 blend，ShaderInstance.apply 将 BlendMode 缓存切为 opaque；原版暗角先设置 ZERO/ONE_MINUS_SRC_COLOR 特殊混合，但 position_tex shader 的标准 alpha blend 随后因缓存差异重设混合，导致暗角原始贴图覆盖场景。修复前真实像素采样复现：暗角前中心亮度 204，暗角后 0；持续效果中也是 190→0。

修复：为 edp_screen.json 声明与原版 position_tex 一致的标准 alpha blend，避免 opaque→alpha 缓存切换破坏暗角特殊混合。屏幕 shader 输出 alpha 固定1，因此仍完整输出原有场景后处理；没有删除或削弱白闪、色差、暗角计算与衰减。ScreenFx 绘制前 flush，保存 shader/texture/color，使用 finally 恢复 GUI 状态并清除场景拷贝有效标记；增加受 debugLogging 控制的英文诊断。

回归在真实客户端和独立世界副本中进行，包括基线、短白闪、持续色差/暗角、消退以及生命值降到49%触发的真实二阶段护盾爆炸。修复后共采样2206帧，暗角覆盖导致中心异常变黑的判定为0；白闪时中心204→204，持续效果与消退时190→190。阶段二实际复活/关联十颗水晶并引爆五颗，收到了爆炸消息。场景和 HUD 保持可见，原有后处理仍存在。

证据：[修复前结果](validation/1201forge/screen-before-results.txt)、[修复后结果](validation/1201forge/screen-after-results.txt)、[关键日志](validation/1201forge/screen-log-excerpt.txt)、[同场景前后截图](validation/1201forge/screen-before-after.png)、[二阶段爆炸后截图](validation/1201forge/screen-stage-two.png)。像素判定针对本次黑白覆盖故障，不代表所有画面或其它模组兼容性已穷尽测试。

测试程序保留在 D:/AI临时文件/edp1201-screenbug；正式 JAR 经 clean build 并检查不含任何验证类和展示类。已运行的旧客户端需要重启才能完整应用修复。
