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
- SHA-256：`177608ee3763d2241bfb49dffd155fb6be951d7ebe54d03aa40adc0475a3d23f`
- 仅完成本地迁移和验证，没有上传 CurseForge 或推送远程。
