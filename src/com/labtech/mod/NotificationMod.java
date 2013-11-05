package com.labtech.mod;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class NotificationMod implements IXposedHookZygoteInit,
		IXposedHookLoadPackage {
	private static final String PACKAGE_NAME = NotificationMod.class
			.getPackage().getName();
	private SharedPreferences prefs;

	// View.STATUS_BAR_DISABLE_EXPAND value
	private static final int STATUS_BAR_DISABLE_EXPAND = 0x00010000;

	// API version belown 17 (4.2) is not having quicksettings
	private static boolean isQSSupportted;

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals(Misc.SYSTEM_UI)) {
			ClassLoader cLoader = lpparam.classLoader;
			if (prefs.getBoolean(Misc.PREF_NL, true)) {
				try {
					// For enabling Notification all time
					XposedHelpers.findAndHookMethod(Misc.PHONESTATUSBAR,
							cLoader, Misc.DISABLE, Misc.INT, new DisableHook());
				} catch (Exception e) {
					XposedBridge.log("Unable to hook to enable notification");
					XposedBridge.log(e);
				}

				try {
					// For Disabling Settings and Quicksettings toggle button
					XposedHelpers.findAndHookMethod(Misc.PANELBAR, cLoader,
							Misc.ONTOUCHEVENT, Misc.MOTIONEVENT,
							new LockscreenNotiHook());
				} catch (Exception e) {
					XposedBridge.log("Unable to hook to disable toggles");
					XposedBridge.log(e);
				}

				if (isQSSupportted)
					try {
						// For directly pulling quicksettings if only
						// Quicksettings
						// is selected
						if (prefs.getString(Misc.PREF_OP, Misc.OP_BOTH).equals(
								Misc.OP_QUICKSETTINGS)) {
							XposedHelpers.findAndHookMethod(
									Misc.NOTIFICATIONPANELVIEW, cLoader,
									Misc.ONTOUCHEVENT, Misc.MOTIONEVENT,
									new AvoidNoti());
						}
					} catch (Exception e) {
						XposedBridge.log("Unable to hook to direct panel");
						XposedBridge.log(e);
					}

			}
			try {
				// For triggering battery icon as power button
				if (prefs.getBoolean(Misc.PREF_BAPM, true)) {
					XposedHelpers.findAndHookMethod(Misc.BATTERYCONTROLLER,
							cLoader, Misc.ADDICONVIEW, Misc.IMAGEVIEW,
							new BAPM(cLoader));
				}
			} catch (Exception e) {
				XposedBridge.log("Unable to hook Battery APM");
				XposedBridge.log(e);
			}
			if (isQSSupportted)
				try {
					// For getting AOSP style dragging.
					if (prefs.getBoolean(Misc.PREF_AOSP, true)) {
						XposedHelpers.findAndHookMethod(
								Misc.NOTIFICATIONPANELVIEW, cLoader,
								Misc.ONTOUCHEVENT, Misc.MOTIONEVENT,
								new AOSPDragStyleHook());
					}
				} catch (Exception e) {
					XposedBridge.log("Unable to hook for AOSP dragger");
					XposedBridge.log(e);
				}

		}
	}

	@Override
	public void initZygote(IXposedHookZygoteInit.StartupParam startupParam)
			throws Throwable {
		prefs = new XSharedPreferences(PACKAGE_NAME, Misc.PREFERENCE);
		isQSSupportted = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
	}

	private class AOSPDragStyleHook extends XC_MethodHook {
		private Object mStatusBar;
		private Method switchToSettings;

		protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param)
				throws Throwable {
			Context context = (Context) XposedHelpers.getObjectField(
					param.thisObject, Misc.MCONTEXT);
			String isIt = prefs.getString(Misc.PREF_OP, Misc.OP_BOTH);

			if ((isIt.equals(Misc.OP_NOTIFICATION)) && isLocked(context))
				return;
			MotionEvent event = (MotionEvent) param.args[0];
			View mHandleView = (View) XposedHelpers.findField(
					param.thisObject.getClass(), "mHandleView").get(
					param.thisObject);
			// Checking if the touch is inside the trigger area
			if (getDragOverlayRect(mHandleView).contains((int) event.getX(),
					(int) event.getY())) {
				if (switchToSettings == null) {
					mStatusBar = XposedHelpers.findField(
							param.thisObject.getClass(), Misc.MSTATUSBAR).get(
							param.thisObject);
					switchToSettings = mStatusBar.getClass().getDeclaredMethod(
							Misc.SWITCHTOSETTINGS, new Class[0]);
				}

				if ((mStatusBar != null) && (switchToSettings != null))
					switchToSettings.invoke(mStatusBar, new Object[0]);
			}
		}

	}

	private class BAPM extends XC_MethodHook {
		ClassLoader cx;

		public BAPM(ClassLoader cx) {
			this.cx = cx;
		}

		@Override
		protected void afterHookedMethod(MethodHookParam param)
				throws Throwable {
			ImageView a = (ImageView) param.args[0];
			final GestureDetector ls = new GestureDetector(a.getContext(),
					new ButtonTouchListener(cx));
			a.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return ls.onTouchEvent(event);
				}
			});
		}
	}

	private class ButtonTouchListener extends
			GestureDetector.SimpleOnGestureListener {

		private Object b;

		public ButtonTouchListener(ClassLoader cx) throws Exception {
			Class<?> x = XposedHelpers.findClass(
					"android.hardware.input.InputManager", cx);
			Method m = x.getMethod("getInstance", new Class[0]);
			b = m.invoke(m, new Object[0]);
		}

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		// Double tapping to turn the screen off
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			try {
				Runtime.getRuntime().exec("input keyevent 26");
			} catch (Exception e1) {
			}
			return true;
		}

		// Holding to trigger APM
		@Override
		public void onLongPress(MotionEvent e) {
			try {
				XposedHelpers.callMethod(b, "injectInputEvent", new KeyEvent(
						KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_POWER), 0);
				// waiting 3secs before lifting the button up to act as a long
				// press
				Thread.sleep(3000);
				XposedHelpers.callMethod(b, "injectInputEvent", new KeyEvent(
						KeyEvent.ACTION_UP, KeyEvent.KEYCODE_POWER), 0);
			} catch (InterruptedException ex) {
			}
			super.onLongPress(e);
		}
	}

	private class LockscreenNotiHook extends XC_MethodHook {

		private View mSettingsButton;
		private View mNotificationButton;

		@SuppressWarnings("rawtypes")
		@Override
		protected void beforeHookedMethod(MethodHookParam param)
				throws Throwable {
			Context context = (Context) XposedHelpers.getObjectField(
					param.thisObject, Misc.MCONTEXT);

			boolean locked = isLocked(context);
			String option = prefs.getString(Misc.PREF_OP, Misc.OP_BOTH);
			boolean hideSettings = prefs.getBoolean(Misc.PREF_DISABLE_SETTINGS,
					true);
			if (!option.equals(Misc.OP_BOTH)) {
				Object mPanels = ((ArrayList) XposedHelpers.findField(
						param.thisObject.getClass(), Misc.MPANELS).get(
						param.thisObject)).get(0);
				Object mStatusBar = XposedHelpers.findField(mPanels.getClass(),
						Misc.MSTATUSBAR).get(mPanels);
				mSettingsButton = (View) XposedHelpers.findField(
						mStatusBar.getClass(), Misc.MSETTINGSBUTTON).get(
						mStatusBar);
				if (isQSSupportted)
					mNotificationButton = (View) XposedHelpers.findField(
							mStatusBar.getClass(), Misc.MNOTIFICATIONBUTTON)
							.get(mStatusBar);
				// adding and removing listeners to hide button according to
				// device lock state
				if (locked) {
					if (option.equals(Misc.OP_NOTIFICATION)
							&& mSettingsButton != null && hideSettings)
						mSettingsButton
								.addOnLayoutChangeListener(changeListener);
					if (option.equals(Misc.OP_QUICKSETTINGS)
							&& mNotificationButton != null && isQSSupportted)
						mNotificationButton
								.addOnLayoutChangeListener(changeListener);
				} else {
					if (option.equals(Misc.OP_NOTIFICATION)
							&& mSettingsButton != null)
						mSettingsButton
								.removeOnLayoutChangeListener(changeListener);
					if (option.equals(Misc.OP_QUICKSETTINGS)
							&& mNotificationButton != null && isQSSupportted)
						mNotificationButton
								.removeOnLayoutChangeListener(changeListener);
				}

			}
		}
	}

	private class DisableHook extends XC_MethodHook {
		@Override
		protected void beforeHookedMethod(MethodHookParam param)
				throws Throwable {
			// If set to disable lockscreen, pull that command out
			if (((Integer) (param.args[0]) & STATUS_BAR_DISABLE_EXPAND) != 0)
				param.args[0] = ((Integer) (param.args[0]))
						- STATUS_BAR_DISABLE_EXPAND;
		}

	}

	// direct pull towards quicksettings
	private class AvoidNoti extends XC_MethodHook {
		private Object mStatusBar;
		private Method switchToSettings;

		@Override
		protected void beforeHookedMethod(MethodHookParam param)
				throws Throwable {
			Context context = (Context) XposedHelpers.getObjectField(
					param.thisObject, Misc.MCONTEXT);

			boolean locked = isLocked(context);
			if (locked) {
				if (switchToSettings == null) {
					mStatusBar = XposedHelpers.findField(
							param.thisObject.getClass(), Misc.MSTATUSBAR).get(
							param.thisObject);
					switchToSettings = mStatusBar.getClass().getDeclaredMethod(
							Misc.SWITCHTOSETTINGS, new Class[0]);
				}

				if ((mStatusBar != null) && (switchToSettings != null))
					switchToSettings.invoke(mStatusBar, new Object[0]);
			}
		}
	}

	// get the lock state
	private boolean isLocked(Context context) {
		KeyguardManager km = (KeyguardManager) context
				.getSystemService(Context.KEYGUARD_SERVICE);
		return km.inKeyguardRestrictedInputMode();
	}

	// button get visible randomly so using layout change listener to disable or
	// enable it again.
	private OnLayoutChangeListener changeListener = new OnLayoutChangeListener() {

		@Override
		public void onLayoutChange(View v, int left, int top, int right,
				int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
			if (isLocked(v.getContext())) {
				v.setVisibility(8);
			} else {
				v.setVisibility(0);
			}
		}
	};

	// dragger area and location
	private Rect getDragOverlayRect(View v) {
		String location = prefs.getString(Misc.PREF_AOSPLOC, Misc.RIGHT);
		int percent = prefs.getInt(Misc.PREF_AOSPPERCENT, 25);
		int size = Math.round((percent * v.getWidth()) / 100);
		int left = 0;
		int right = 0;
		if (location.equals(Misc.RIGHT)) {
			left = v.getWidth() - size;
			right = v.getWidth();
		} else if (location.equals(Misc.LEFT)) {
			right = size;
		} else if (location.equals(Misc.CENTER)) {
			int width_h = v.getWidth() / 2;
			int size_h = size / 2;
			left = width_h - size_h;
			right = width_h + size_h;
		}
		Rect checkRect = new Rect(left, 0, right, v.getHeight());
		return checkRect;
	}

}