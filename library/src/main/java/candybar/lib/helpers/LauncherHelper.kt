package candybar.lib.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Pair
import android.widget.Toast
import androidx.annotation.DrawableRes
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import com.afollestad.materialdialogs.MaterialDialog

/*
 * CandyBar - Material Dashboard (Part 1)
 */

object LauncherHelper {

    private const val thirdPartyHelperURL = "https://play.google.com/store/apps/details?id=rk.android.app.shortcutmaker"

    private val NO_SETTINGS_ACTIVITY: String? = null
    private val DIRECT_APPLY_NOT_SUPPORTED: LauncherType.DirectApply? = null
    private val MANUAL_APPLY_NOT_SUPPORTED: LauncherType.ManualApply? = null
    private val DEFAULT_CALLBACK = LauncherType.ApplyCallback { context ->
        if (context is Activity) context.finish()
    }

    enum class LauncherType(
        val launcherName: String?,
        @DrawableRes val icon: Int,
        val packages: Array<String>?,
        val settingsActivityName: String?,
        private var directApplyFunc: DirectApply? = null,
        private var manualApplyFunc: ManualApply? = null
    ) {
        UNKNOWN(null, 0, null, null),

        ACTION(
            "Action", R.drawable.ic_launcher_action, arrayOf("com.actionlauncher.playstore", "com.chrislacy.actionlauncher.pro"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = context.packageManager.getLaunchIntentForPackage(launcherPackageName)
                    ?.putExtra("apply_icon_pack", context.packageName)?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }),
        ADW(
            "ADW", R.drawable.ic_launcher_adw, arrayOf("org.adw.launcher", "org.adwfreak.launcher"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("org.adw.launcher.SET_THEME")
                    .putExtra("org.adw.launcher.theme.NAME", context.packageName).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }),
        APEX(
            "Apex", R.drawable.ic_launcher_apex, arrayOf("com.anddoes.launcher", "com.anddoes.launcher.pro"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("com.anddoes.launcher.SET_THEME")
                    .putExtra("com.anddoes.launcher.THEME_PACKAGE_NAME", context.packageName).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }),
        BEFORE(
            "Before", R.drawable.ic_launcher_before, arrayOf("com.beforesoft.launcher"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("com.beforesoftware.launcher.APPLY_ICONS")
                    .putExtra("packageName", context.packageName).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            object : ManualApply {
                override fun getCompatibilityMessage(context: Context, launcherName: String) = context.resources.getString(R.string.apply_manual_before)
                override fun getInstructionSteps(context: Context, launcherName: String) = arrayOf(
                    context.resources.getString(R.string.apply_manual_before_step_1), context.resources.getString(R.string.apply_manual_before_step_2),
                    context.resources.getString(R.string.apply_manual_before_step_3), context.resources.getString(R.string.apply_manual_before_step_4),
                    context.resources.getString(R.string.apply_manual_before_step_5, context.resources.getString(R.string.app_name))
                )
            }),
        BLACKBERRY(
            "BlackBerry", R.drawable.ic_launcher_blackberry, arrayOf("com.blackberry.blackberrylauncher"), "com.blackberry.blackberrylauncher.MainActivity",
            DIRECT_APPLY_NOT_SUPPORTED, object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = emptyArray<String>()
            }),
        CMTHEME(
            "CM Theme", R.drawable.ic_launcher_cm, arrayOf("org.cyanogenmod.theme.chooser"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("android.intent.action.MAIN")
                    .setComponent(ComponentName(launcherPackageName, "org.cyanogenmod.theme.chooser.ChooserActivity")).putExtra("pkgName", context.packageName)

                override fun run(context: Context, launcherPackageName: String, callback: ApplyCallback?) {
                    try {
                        super.run(context, launcherPackageName, callback)
                    } catch (e: Exception) {
                        when (e) {
                            is ActivityNotFoundException, is NullPointerException -> Toast.makeText(context, R.string.apply_cmtheme_not_available, Toast.LENGTH_LONG).show()
                            is SecurityException, is IllegalArgumentException -> Toast.makeText(context, R.string.apply_cmtheme_failed, Toast.LENGTH_LONG).show()
                            else -> throw e
                        }
                    }
                }
            }),
        COLOR_OS(
            "ColorOS", R.drawable.ic_launcher_color_os, arrayOf("com.oppo.launcher"), NO_SETTINGS_ACTIVITY, DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun isSupported(launcherPackageName: String) = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                override fun getInstructionSteps(context: Context, launcherName: String) = arrayOf(
                    context.resources.getString(R.string.apply_manual_color_os_step_1), context.resources.getString(R.string.apply_manual_color_os_step_2),
                    context.resources.getString(R.string.apply_manual_color_os_step_3), context.resources.getString(R.string.apply_manual_color_os_step_4, context.resources.getString(R.string.app_name))
                )

                override fun run(context: Context, launcherPackageName: String, callback: ApplyCallback?) {
                    if (isSupported(launcherPackageName)) super.run(context, launcherPackageName, callback)
                    else launcherIncompatibleCustomMessage(context, "ColorOS", context.resources.getString(R.string.apply_launcher_incompatible_depending_on_version, "ColorOS", 10))
                }
            }),
        FLICK(
            "Flick", R.drawable.ic_launcher_flick, arrayOf("com.universallauncher.universallauncher"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = context.packageManager.getLaunchIntentForPackage("com.universallauncher.universallauncher")?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                override fun getBroadcast(context: Context) = Intent("com.universallauncher.universallauncher.FLICK_ICON_PACK_APPLIER")
                    .putExtra("com.universallauncher.universallauncher.ICON_THEME_PACKAGE", context.packageName)
                    .setComponent(ComponentName("com.universallauncher.universallauncher", "com.android.launcher3.icon.ApplyIconPack"))
            }),
        GO(
            "GO EX", R.drawable.ic_launcher_go, arrayOf("com.gau.go.launcherex"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = context.packageManager.getLaunchIntentForPackage("com.gau.go.launcherex")?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                override fun getBroadcast(context: Context) = Intent("com.gau.go.launcherex.MyThemes.mythemeaction").putExtra("type", 1).putExtra("pkgname", context.packageName)
            }),
        HIOS(
            "HiOS", R.drawable.ic_launcher_hios, arrayOf("com.transsion.hilauncher"), NO_SETTINGS_ACTIVITY, DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = arrayOf(
                    context.resources.getString(R.string.apply_manual_hios_step_1), context.resources.getString(R.string.apply_manual_hios_step_2),
                    context.resources.getString(R.string.apply_manual_hios_step_3), context.resources.getString(R.string.apply_manual_hios_step_4),
                    context.resources.getString(R.string.apply_manual_hios_step_5, context.resources.getString(R.string.app_name))
                )
            }),
        HOLO(
            "Holo", R.drawable.ic_launcher_holo, arrayOf("com.mobint.hololauncher"), "com.mobint.hololauncher.SettingsActivity", DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = emptyArray<String>()
            }),
        HOLOHD(
            "Holo HD", R.drawable.ic_launcher_holohd, arrayOf("com.mobint.hololauncher.hd"), "com.mobint.hololauncher.SettingsActivity", DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = emptyArray<String>()
            }),
        HYPERION(
            "Hyperion", R.drawable.ic_launcher_hyperion, arrayOf("projekt.launcher"), "projekt.launcher.activities.SettingsActivity", DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = emptyArray<String>()
            }),
        ION_LAUNCHER(
            "Ion Launcher", R.drawable.ic_launcher_ion, arrayOf("one.zagura.IonLauncher"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("one.zagura.IonLauncher.ui.settings.iconPackPicker.IconPackPickerActivity")
                    .putExtra("pkgname", context.packageName).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }),
        KISS(
            "KISS", R.drawable.ic_launcher_kiss, arrayOf("fr.neamar.kiss"), NO_SETTINGS_ACTIVITY, DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = arrayOf(
                    context.resources.getString(R.string.apply_manual_kiss_step_1), context.resources.getString(R.string.apply_manual_kiss_step_2),
                    context.resources.getString(R.string.apply_manual_kiss_step_3), context.resources.getString(R.string.apply_manual_kiss_step_4, context.resources.getString(R.string.app_name))
                )
            }),
        KVAESITSO(
            "Kvaesitso", R.drawable.ic_launcher_kvaesitso, arrayOf("de.mm20.launcher2.release"), NO_SETTINGS_ACTIVITY, DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = arrayOf(
                    context.resources.getString(R.string.apply_manual_kvaesitso_step_1), context.resources.getString(R.string.apply_manual_kvaesitso_step_2),
                    context.resources.getString(R.string.apply_manual_kvaesitso_step_3), context.resources.getString(R.string.apply_manual_kvaesitso_step_4, context.resources.getString(R.string.app_name)),
                    context.resources.getString(R.string.apply_manual_kvaesitso_step_5)
                )
            }),
        LAWNCHAIR_LEGACY(
            "Lawnchair Legacy", R.drawable.ic_launcher_lawnchair, arrayOf("ch.deletescape.lawnchair.plah", "ch.deletescape.lawnchair.ci"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("ch.deletescape.lawnchair.APPLY_ICONS").putExtra("packageName", context.packageName)
            }, object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = emptyArray<String>()
            }),
        LAWNCHAIR(
            "Lawnchair", R.drawable.ic_launcher_lawnchair, arrayOf("app.lawnchair", "app.lawnchair.play"), "app.lawnchair.ui.preferences.PreferenceActivity",
            DIRECT_APPLY_NOT_SUPPORTED, object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = emptyArray<String>()
            }),
        LGHOME("LG Home", R.drawable.ic_launcher_lg, arrayOf("com.lge.launcher2", "com.lge.launcher3"), NO_SETTINGS_ACTIVITY, DIRECT_APPLY_NOT_SUPPORTED, MANUAL_APPLY_NOT_SUPPORTED),
        LUCID(
            "Lucid", R.drawable.ic_launcher_lucid, arrayOf("com.powerpoint45.launcher"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("com.powerpoint45.action.APPLY_THEME", null).putExtra("icontheme", context.packageName)
            }, MANUAL_APPLY_NOT_SUPPORTED
        ),
        MOTO(
            "Moto Launcher", R.drawable.ic_launcher_moto, arrayOf("com.motorola.launcher3"), "com.motorola.personalize/com.motorola.personalize.app.IconPacksActivity",
            DIRECT_APPLY_NOT_SUPPORTED, object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = emptyArray<String>()
            }),
        MICROSOFT(
            "Microsoft", R.drawable.ic_launcher_microsoft, arrayOf("com.microsoft.launcher"), NO_SETTINGS_ACTIVITY, DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = emptyArray<String>()
            }),
        MLAUNCHER(
            "mLauncher", R.drawable.ic_launcher_mlauncher, arrayOf("app.mlauncher"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("app.mlauncher.APPLY_ICONS", null).putExtra("packageName", context.packageName)
            }, MANUAL_APPLY_NOT_SUPPORTED
        ),
        NIAGARA(
            "Niagara", R.drawable.ic_launcher_niagara, arrayOf("bitpit.launcher"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("bitpit.launcher.APPLY_ICONS").putExtra("packageName", context.packageName)
            }, MANUAL_APPLY_NOT_SUPPORTED
        ),
        NOTHING(
            "Nothing", R.drawable.ic_launcher_nothing, arrayOf("com.nothing.launcher"), NO_SETTINGS_ACTIVITY, DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = arrayOf(
                    context.resources.getString(R.string.apply_manual_nothing_step_1), context.resources.getString(R.string.apply_manual_nothing_step_2),
                    context.resources.getString(R.string.apply_manual_nothing_step_3), context.resources.getString(R.string.apply_manual_nothing_step_4, context.resources.getString(R.string.app_name))
                )
            }),
        NOUGAT(
            "Nougat", R.drawable.ic_launcher_nougat, arrayOf("me.craftsapp.nlauncher"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("me.craftsapp.nlauncher").setAction("me.craftsapp.nlauncher.SET_THEME")
                    .putExtra("me.craftsapp.nlauncher.theme.NAME", context.packageName).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }, MANUAL_APPLY_NOT_SUPPORTED
        ),
        NOVA(
            "Nova", R.drawable.ic_launcher_nova, arrayOf("com.teslacoilsw.launcher", "com.teslacoilsw.launcher.prime"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String): Intent {
                    val nova = Intent("com.teslacoilsw.launcher.APPLY_ICON_THEME").setPackage("com.teslacoilsw.launcher")
                        .putExtra("com.teslacoilsw.launcher.extra.ICON_THEME_TYPE", "GO").putExtra("com.teslacoilsw.launcher.extra.ICON_THEME_PACKAGE", context.packageName)
                    val reshapeSetting = context.resources.getString(R.string.nova_reshape_legacy_icons)
                    if (reshapeSetting != "KEEP") nova.putExtra("com.teslacoilsw.launcher.extra.ICON_THEME_RESHAPE", reshapeSetting)
                    return nova.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }, MANUAL_APPLY_NOT_SUPPORTED
        ),
        ONEUI(
            "Samsung One UI", R.drawable.ic_launcher_one_ui, arrayOf("com.sec.android.app.launcher"), NO_SETTINGS_ACTIVITY, DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun isSupported(launcherPackageName: String) = Build.VERSION.SDK_INT > Build.VERSION_CODES.R
                override fun getCompatibilityMessage(context: Context, launcherName: String) = context.resources.getString(R.string.apply_manual_samsung_oneui, launcherName, "$launcherName 4.0")
                override fun getInstructionSteps(context: Context, launcherName: String) = arrayOf(
                    context.resources.getString(R.string.apply_manual_samsung_oneui_step_1, "Samsung Galaxy Store"), context.resources.getString(R.string.apply_manual_samsung_oneui_step_2, "Theme Park"),
                    context.resources.getString(R.string.apply_manual_samsung_oneui_step_3), context.resources.getString(R.string.apply_manual_samsung_oneui_step_4),
                    context.resources.getString(R.string.apply_manual_samsung_oneui_step_5), context.resources.getString(R.string.apply_manual_samsung_oneui_step_6, context.resources.getString(R.string.app_name)),
                    context.resources.getString(R.string.apply_manual_samsung_oneui_step_7, context.resources.getString(R.string.app_name))
                )

                override fun run(context: Context, launcherPackageName: String, callback: ApplyCallback?) = applyOneUI(context, launcherPackageName, callback)
            }),
        OXYGEN_OS(
            "OxygenOS", R.drawable.ic_launcher_oxygen_os, arrayOf("net.oneplus.launcher"), NO_SETTINGS_ACTIVITY, DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun isSupported(launcherPackageName: String) = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                override fun getInstructionSteps(context: Context, launcherName: String) = arrayOf(
                    context.resources.getString(R.string.apply_manual_oxygen_os_step_1), context.resources.getString(R.string.apply_manual_oxygen_os_step_2),
                    context.resources.getString(R.string.apply_manual_oxygen_os_step_3), context.resources.getString(R.string.apply_manual_oxygen_os_step_4, context.resources.getString(R.string.app_name))
                )

                override fun run(context: Context, launcherPackageName: String, callback: ApplyCallback?) {
                    if (isSupported(launcherPackageName)) super.run(context, launcherPackageName, callback)
                    else launcherIncompatibleCustomMessage(context, "OxygenOS", context.resources.getString(R.string.apply_launcher_incompatible_depending_on_version, "OxygenOS", 8))
                }
            }),
        PIXEL("Pixel", R.drawable.ic_launcher_pixel, arrayOf("com.google.android.apps.nexuslauncher"), NO_SETTINGS_ACTIVITY, DIRECT_APPLY_NOT_SUPPORTED, MANUAL_APPLY_NOT_SUPPORTED),
        POCO(
            "POCO", R.drawable.ic_launcher_poco, arrayOf("com.mi.android.globallauncher"), "com.miui.home.settings.HomeSettingsActivity", DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = emptyArray<String>()
            }),
        PROJECTIVY(
            "Projectivy", R.drawable.ic_launcher_projectivy, arrayOf("com.spocky.projengmenu"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("com.spocky.projengmenu.APPLY_ICONPACK").setPackage("com.spocky.projengmenu")
                    .putExtra("com.spocky.projengmenu.extra.ICONPACK_PACKAGENAME", context.packageName).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }, MANUAL_APPLY_NOT_SUPPORTED
        ),
        SMART(
            "Smart", R.drawable.ic_launcher_smart, arrayOf("ginlemon.flowerfree", "ginlemon.flowerpro", "ginlemon.flowerpro.special"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("ginlemon.smartlauncher.setGSLTHEME").putExtra("package", context.packageName).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }, MANUAL_APPLY_NOT_SUPPORTED
        ),
        SOLO(
            "Solo", R.drawable.ic_launcher_solo, arrayOf("home.solo.launcher.free"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = context.packageManager.getLaunchIntentForPackage("home.solo.launcher.free")?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                override fun getBroadcast(context: Context) = Intent("home.solo.launcher.free.APPLY_THEME").putExtra("EXTRA_THEMENAME", context.resources.getString(R.string.app_name)).putExtra("EXTRA_PACKAGENAME", context.packageName)
            }, MANUAL_APPLY_NOT_SUPPORTED
        ),
        SQUARE(
            "Square", R.drawable.ic_launcher_square, arrayOf("com.ss.squarehome2"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("com.ss.squarehome2.ACTION_APPLY_ICONPACK")
                    .setComponent(ComponentName.unflattenFromString("com.ss.squarehome2/.ApplyThemeActivity")).putExtra("com.ss.squarehome2.EXTRA_ICONPACK", context.packageName)
            }, MANUAL_APPLY_NOT_SUPPORTED
        ),
        STOCK_LEGACY(
            if (isColorOS()) "ColorOS" else if (isRealmeUI()) "realme UI" else "Stock Launcher",
            if (isColorOS()) R.drawable.ic_launcher_color_os else if (isRealmeUI()) R.drawable.ic_launcher_realme_ui else R.drawable.ic_launcher_android,
            arrayOf("com.android.launcher"),
            NO_SETTINGS_ACTIVITY,
            DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = arrayOf(
                    context.resources.getString(R.string.apply_manual_color_os_step_1), context.resources.getString(R.string.apply_manual_color_os_step_2),
                    context.resources.getString(R.string.apply_manual_color_os_step_3), context.resources.getString(R.string.apply_manual_color_os_step_4, context.resources.getString(R.string.app_name))
                )
            }),
        TINYBIT(
            "TinyBit", R.drawable.ic_launcher_tinybit, arrayOf("rocks.tbog.tblauncher"), "rocks.tbog.tblauncher.SettingsActivity", DIRECT_APPLY_NOT_SUPPORTED,
            object : ManualApply {
                override fun getInstructionSteps(context: Context, launcherName: String) = emptyArray<String>()
            }),
        ZENUI(
            "ZenUI", R.drawable.ic_launcher_zenui, arrayOf("com.asus.launcher"), NO_SETTINGS_ACTIVITY,
            object : DirectApply {
                override fun getActivity(context: Context, launcherPackageName: String) = Intent("com.asus.launcher")
                    .setAction("com.asus.launcher.intent.action.APPLY_ICONPACK").addCategory(Intent.CATEGORY_DEFAULT)
                    .putExtra("com.asus.launcher.iconpack.PACKAGE_NAME", context.packageName).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            });

        interface DirectApply {
            fun isSupported(packageName: String): Boolean = true
            fun getActivity(context: Context, launcherPackageName: String): Intent?
            fun getBroadcast(context: Context): Intent? = null

            @Throws(ActivityNotFoundException::class, NullPointerException::class)
            fun run(context: Context, launcherPackageName: String, callback: ApplyCallback?) {
                val activityIntent = getActivity(context, launcherPackageName)
                getBroadcast(context)?.let { context.sendBroadcast(it) }
                activityIntent?.let { context.startActivity(it) }
                callback?.onSuccess(context)
            }
        }

        interface ManualApply {
            fun isSupported(launcherPackageName: String): Boolean = true
            fun getCompatibilityMessage(context: Context, launcherName: String) = context.resources.getString(R.string.apply_manual, launcherName, context.resources.getString(R.string.app_name))
            fun getInstructionSteps(context: Context, launcherName: String): Array<String>

            @Throws(ActivityNotFoundException::class, NullPointerException::class)
            fun run(context: Context, launcherPackageName: String, callback: ApplyCallback?) = showManualApplyDialog(context, launcherPackageName, callback)
        }

        fun interface ApplyCallback {
            fun onSuccess(context: Context)
        }

        fun isInstalled(context: Context): Boolean {
            if (packages == null) return false
            for (p in packages) {
                try {
                    context.packageManager.getPackageInfo(p, 0); return true
                } catch (e: Exception) {
                }
            }
            return false
        }

        fun getDirectApplyFunc() = directApplyFunc
        fun getManualApplyFunc() = manualApplyFunc
    }

    class Launcher(val type: LauncherType, val installedPackage: String) {
        fun supportsDirectApply(): Boolean = type.getDirectApplyFunc()?.isSupported(installedPackage) ?: false
        fun supportsManualApply(): Boolean = type.getManualApplyFunc()?.isSupported(installedPackage) ?: false
        fun supportsIconPacks() = supportsDirectApply() || supportsManualApply()
        fun getManualApplyInstructions(context: Context) = type.getManualApplyFunc()?.getInstructionSteps(context, type.launcherName ?: "") ?: emptyArray()
        fun getDirectApplyIntents(context: Context): Pair<Intent, Intent>? {
            val directApply = type.getDirectApplyFunc() ?: return null
            return Pair(directApply.getActivity(context, installedPackage), directApply.getBroadcast(context))
        }

        @Throws(ActivityNotFoundException::class, NullPointerException::class)
        fun applyDirectly(context: Context, callback: LauncherType.ApplyCallback) {
            if (!isInstalled(context)) throw LauncherNotInstalledException(ActivityNotFoundException())
            val directApply = type.getDirectApplyFunc() ?: throw LauncherDirectApplyNotSupported(ActivityNotFoundException())
            if (!directApply.isSupported(installedPackage)) throw LauncherDirectApplyNotSupported(ActivityNotFoundException())
            try {
                directApply.run(context, installedPackage, callback); logLauncherDirectApply(installedPackage)
            } catch (e: Exception) {
                throw LauncherDirectApplyFailed(e)
            }
        }

        @Throws(ActivityNotFoundException::class, NullPointerException::class)
        fun applyDirectly(context: Context) = applyDirectly(context, DEFAULT_CALLBACK)

        @Throws(ActivityNotFoundException::class, NullPointerException::class)
        fun applyManually(context: Context, callback: LauncherType.ApplyCallback) {
            val manualApply = type.getManualApplyFunc() ?: throw LauncherManualApplyNotSupported(ActivityNotFoundException())
            if (!manualApply.isSupported(installedPackage)) throw LauncherManualApplyNotSupported(ActivityNotFoundException())
            try {
                manualApply.run(context, installedPackage, callback); logLauncherManualApply(installedPackage, "confirm")
            } catch (e: Exception) {
                throw LauncherManualApplyFailed(e)
            }
        }

        @Throws(ActivityNotFoundException::class, NullPointerException::class)
        fun applyManually(context: Context) = applyManually(context, DEFAULT_CALLBACK)

        fun isInstalled(context: Context): Boolean {
            return try {
                context.packageManager.getPackageInfo(installedPackage, 0); true
            } catch (e: Exception) {
                false
            }
        }

        @SuppressLint("StringFormatInvalid")
        fun apply(context: Context) {
            val packageName = installedPackage
            val launcherName = type.launcherName ?: ""
            CandyBarApplication.getConfiguration().analyticsHandler?.logEvent("click", hashMapOf("section" to "apply", "action" to "open_dialog", "launcher" to packageName))
            if (!supportsIconPacks()) {
                launcherIncompatible(context, launcherName); return
            }
            if (!isInstalled(context) && !supportsManualApply()) {
                showInstallPrompt(context, packageName); return
            }
            if (supportsDirectApply()) {
                try {
                    applyDirectly(context); return
                } catch (e: Exception) {
                }
            }
            if (supportsManualApply()) {
                try {
                    applyManually(context); return
                } catch (e: Exception) {
                }
            }
            launcherIncompatible(context, launcherName)
        }

        class LauncherNotInstalledException(cause: Throwable) : ActivityNotFoundException("The launcher is not installed") { init {
            initCause(cause)
        }
        }

        class LauncherDirectApplyNotSupported(cause: Throwable) : ActivityNotFoundException("Direct apply not supported") { init {
            initCause(cause)
        }
        }

        class LauncherManualApplyNotSupported(cause: Throwable) : ActivityNotFoundException("Manual apply not supported") { init {
            initCause(cause)
        }
        }

        class LauncherDirectApplyFailed(cause: Throwable) : ActivityNotFoundException("Direct apply failed") { init {
            initCause(cause)
        }
        }

        class LauncherManualApplyFailed(cause: Throwable) : ActivityNotFoundException("Manual apply failed") { init {
            initCause(cause)
        }
        }
    }

    @JvmStatic
    fun getLauncher(packageName: String?): Launcher {
        if (packageName == null) return Launcher(LauncherType.UNKNOWN, "")
        for (l in LauncherType.entries) {
            l.packages?.let {
                for (p in it) {
                    if (p == packageName) return Launcher(l, packageName)
                }
            }
        }
        return Launcher(LauncherType.UNKNOWN, packageName)
    }

    @JvmStatic
    private fun logLauncherDirectApply(launcherPackage: String) {
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent("click", hashMapOf("section" to "apply", "action" to "confirm", "launcher" to launcherPackage))
    }

    @JvmStatic
    private fun logLauncherManualApply(launcherPackage: String, action: String) {
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent("click", hashMapOf("section" to "apply", "action" to "manual_open_$action", "launcher" to launcherPackage))
    }

    @SuppressLint("StringFormatInvalid")
    @JvmStatic
    private fun showManualApplyDialog(context: Context, launcherPackageName: String, callback: LauncherType.ApplyCallback?) {
        val launcher = getLauncher(launcherPackageName)
        val isInstalled = launcher.isInstalled(context)
        val positiveButton = if (isInstalled) R.string.close else R.string.install
        val installPrompt = context.resources.getString(R.string.apply_launcher_not_installed, launcher.type.launcherName)
        val activityLaunchFailed = context.resources.getString(R.string.apply_launch_failed, launcher.type.launcherName)
        val manualApply = launcher.type.getManualApplyFunc()
        val description = manualApply?.getCompatibilityMessage(context, launcher.type.launcherName ?: "")
        val steps = manualApply?.getInstructionSteps(context, launcher.type.launcherName ?: "") ?: emptyArray()
        val content = StringBuilder()
        if (!description.isNullOrEmpty()) content.append(description).append("\n\n")
        if (steps.isNotEmpty()) content.append("\t• ").append(steps.joinToString("\n\t• "))
        if (!isInstalled && steps.isNotEmpty()) content.append("\n\n")
        if (!isInstalled) content.append(installPrompt)

        MaterialDialog.Builder(context)
            .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
            .title(launcher.type.launcherName ?: "").content(content.toString())
            .positiveText(positiveButton).onPositive { _, _ ->
                if (isInstalled) {
                    logLauncherManualApply(launcherPackageName, "confirm")
                    val settingsActivity = launcher.type.settingsActivityName ?: return@onPositive
                    try {
                        val intent = Intent(Intent.ACTION_MAIN).setComponent(
                            if (settingsActivity.contains("/")) {
                                val parts = settingsActivity.split("/"); ComponentName(parts[0], parts[1])
                            } else ComponentName(launcherPackageName, settingsActivity)
                        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent); callback?.onSuccess(context)
                    } catch (e: Exception) {
                        if (e is ActivityNotFoundException || e is NullPointerException) openGooglePlay(context, launcherPackageName) else Toast.makeText(context, activityLaunchFailed, Toast.LENGTH_LONG).show()
                    }
                } else openGooglePlay(context, launcherPackageName)
            }
            .negativeText(android.R.string.cancel).onNegative { _, _ -> logLauncherManualApply(launcherPackageName, "cancel") }.show()
    }

    @JvmStatic
    private fun showInstallPrompt(context: Context, launcherPackageName: String) = showManualApplyDialog(context, launcherPackageName, DEFAULT_CALLBACK)

    @JvmStatic
    private fun applyOneUI(context: Context, launcherPackage: String, callback: LauncherType.ApplyCallback?) {
        val launcher = getLauncher(launcherPackage)
        val launcherName = launcher.type.launcherName ?: ""
        val manualApply = launcher.type.getManualApplyFunc() ?: return
        val instructions = manualApply.getInstructionSteps(context, launcherName)
        val incompatibleText = context.resources.getString(R.string.apply_manual_samsung_oneui_too_old, launcherName)
        val compatibleText = StringBuilder()
        if (instructions.isNotEmpty()) {
            compatibleText.append("\t• ").append(instructions[0]).append("\n\t• ")
            compatibleText.append(instructions.sliceArray(1 until instructions.size - 2).joinToString("\n\t• "))
            compatibleText.append("\n\n").append(instructions.last())
        }

        MaterialDialog.Builder(context)
            .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
            .title(launcherName).content(manualApply.getCompatibilityMessage(context, launcherName) + "\n\n" + (if (manualApply.isSupported(launcherPackage)) compatibleText.toString() else incompatibleText))
            .positiveText(R.string.close).onPositive { _, _ ->
                logLauncherManualApply(launcherPackage, "confirm")
                if (manualApply.isSupported(launcherPackage)) {
                    val packageName = "com.samsung.android.themedesigner"
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("samsungapps://ProductDetail/$packageName"))); callback?.onSuccess(context)
                    } catch (e: Exception) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://galaxystore.samsung.com/detail/$packageName"))); callback?.onSuccess(context)
                        } catch (ignored: Exception) {
                            Toast.makeText(context, context.resources.getString(R.string.no_browser), Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    try {
                        context.startActivity(Intent("android.settings.SYSTEM_UPDATE_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (ignored: Exception) {
                    }
                }
            }
            .negativeText(android.R.string.cancel).onNegative { _, _ -> logLauncherManualApply(launcherPackage, "cancel") }.show()
    }

    @JvmStatic
    private fun launcherIncompatible(context: Context, launcherName: String) {
        launcherIncompatibleCustomMessage(context, launcherName, context.resources.getString(R.string.apply_launcher_incompatible, launcherName, launcherName))
    }

    @JvmStatic
    private fun launcherIncompatibleCustomMessage(context: Context, launcherName: String, message: String) {
        MaterialDialog.Builder(context).typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context)).title(launcherName).content(message)
            .positiveText(R.string.close).onPositive { _, _ ->
                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent("click", hashMapOf("section" to "apply", "action" to "incompatible_third_party_open", "launcher" to launcherName))
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(thirdPartyHelperURL)))
                } catch (e: Exception) {
                    Toast.makeText(context, context.resources.getString(R.string.no_browser), Toast.LENGTH_LONG).show()
                }
            }
            .negativeText(android.R.string.cancel).onNegative { _, _ ->
                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent("click", hashMapOf("section" to "apply", "action" to "incompatible_third_party_cancel", "launcher" to launcherName))
            }.show()
    }

    @JvmStatic
    fun openGooglePlay(context: Context, packageName: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        } catch (e: Exception) {
            Toast.makeText(context, context.resources.getString(R.string.no_browser), Toast.LENGTH_LONG).show()
        }
    }

    @JvmStatic
    fun quickApply(context: Context): Boolean {
        val pm = context.packageManager
        val packageName = pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName ?: return false
        val launcher = getLauncher(packageName)
        return try {
            if (launcher.supportsDirectApply()) {
                launcher.applyDirectly(context, DEFAULT_CALLBACK); true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    @JvmStatic
    fun isColorOS(): Boolean {
        val version = getSystemProperty("ro.build.version.opporom")
        val isLegacy = !version.isNullOrEmpty()
        var isHybrid = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (Build.MANUFACTURER.equals("OnePlus", ignoreCase = true) || Build.MANUFACTURER.equals("OPPO", ignoreCase = true)) isHybrid = true
            else isHybrid = Build.MANUFACTURER.equals("realme", ignoreCase = true) && isLegacy
        }
        return isLegacy || isHybrid
    }

    @JvmStatic
    fun isRealmeUI() = if (Build.MANUFACTURER.equals("realme", ignoreCase = true)) !getSystemProperty("ro.build.version.realmeui").isNullOrEmpty() else false

    @JvmStatic
    fun getSystemProperty(property: String): String? {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            get.invoke(systemProperties, property) as? String
        } catch (ignored: Exception) {
            null
        }
    }
}
