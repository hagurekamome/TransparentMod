package biz.hagurekamome.transparentmod;

import android.content.res.XResources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class TransparentMod implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage{

	private static XSharedPreferences pref = new XSharedPreferences(TransparentMod.class.getPackage().getName());

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

		if (!lpparam.packageName.equals("com.android.systemui"))
			return;

		try {
				XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader,"getNavigationBarLayoutParams", new XC_MethodReplacement(){
					@Override
					protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
						WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
								LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
								0x7e3,
								0
								| WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
								| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
								| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
								| WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
								PixelFormat.TRANSLUCENT);		//PixelFormat.OPAQUEだったのを置き換え
						lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
						lp.setTitle("NavigationBar");
						lp.windowAnimations = 0;
						return lp;
					}
				});
				
				XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader,"makeStatusBarView", new XC_MethodHook(){
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						// mPixelFormat = PixelFormat.OPAQUEを置き換え
						XposedHelpers.setIntField(param.thisObject, "mPixelFormat", PixelFormat.TRANSLUCENT);	
					}
				});

				XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader,"prepareNavigationBarView", new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							LinearLayout mNavigationBarView = (LinearLayout)XposedHelpers.getObjectField(param.thisObject, "mNavigationBarView");
							mNavigationBarView.setBackgroundColor(pref.getInt("status_bar_background", 0xff000000));
							XposedHelpers.setObjectField(param.thisObject, "mNavigationBarView", mNavigationBarView);
						}
					});
		} catch (Throwable t) {
			XposedBridge.log(t);
		}
	
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {

		if (!resparam.packageName.equals("com.android.systemui"))
			return;

		final int status_bar_background = pref.getInt("status_bar_background", 0xff000000);
		final int c_alpha = Color.alpha(status_bar_background);
		final int c_red = Color.red(status_bar_background);
		final int c_green = Color.green(status_bar_background);
		final int c_blue = Color.blue(status_bar_background);

		XResources.DrawableLoader dl = new XResources.DrawableLoader() {
			@Override
			public Drawable newDrawable(XResources res, int id) throws Throwable {
				ColorDrawable cd = new ColorDrawable();
				cd.setColor(Color.argb(c_alpha, c_red, c_green, c_blue));
				return cd;
			}
		};

		try{
			resparam.res.setReplacement("com.android.systemui", "drawable", "status_bar_background", dl);
		} catch (Throwable t) {
			XposedBridge.log(t);
		}
	}

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {

		try{
			Class<?> localClass = XposedHelpers.findClass("com.android.internal.policy.impl.PhoneWindowManager", null);
			Object[] arrayOfObject = new Object[2];
			arrayOfObject[0] = Rect.class;						//getgetSystemDecorRectLwに渡される引数の型
			arrayOfObject[1] = new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
						Rect localRect = (Rect)param.args[0];
						localRect.left = XposedHelpers.getIntField(param.thisObject, "mSystemLeft");
						localRect.right = XposedHelpers.getIntField(param.thisObject, "mSystemRight");
						localRect.top = XposedHelpers.getIntField(param.thisObject, "mSystemTop");
						localRect.bottom = XposedHelpers.getIntField(param.thisObject, "mSystemBottom");
						return Integer.valueOf(0);
				}
			};

			XposedHelpers.findAndHookMethod(localClass, "getSystemDecorRectLw", arrayOfObject);

		} catch (Throwable t) {
			XposedBridge.log(t);
		}
	}
}
