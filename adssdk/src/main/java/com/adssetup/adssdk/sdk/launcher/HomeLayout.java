package com.adssetup.adssdk.sdk.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adssetup.adssdk.sdk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class HomeLayout {
    static final String DEFAULT_FOLDER_NAME = "Folder";
    static final int FOLDER_BG_COLOR = 0x66FFFFFF;
    static final int COLUMNS = 4;
    static final int PER_PAGE = 24;
    static final int DOCK = -1;
    static final int DOCK_SLOTS = 4;
    private static final String PREF_DOCK_INIT = "dock_init_v1";

    @Nullable
    private List<DockItem> dockDefaults;

    static final class Folder {
        final String id;
        String name;
        final List<String> apps = new ArrayList<>();

        Folder(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static final class Slot {
        Object e;
        int page;
        int cell;

        Slot(Object e, int page, int cell) {
            this.e = e;
            this.page = page;
            this.cell = cell;
        }

        String itemKey() {
            return e instanceof String ? "app:" + e : "folder:" + ((Folder) e).id;
        }
    }

    private static final String PREFS = "home_layout";
    private static final String KEY = "layout_v1";
    private static final long SAVE_DELAY_MS = 600;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private final SharedPreferences prefs;
    private final int iconPx;
    private final Context appContext;
    private final boolean allAppsOnHome;
    private static final int LAYOUT_VERSION = 4;
    private int loadedVersion;
    private final AppInfo blockBlast;
    private final List<AppInfo> quizApps = new ArrayList<>();
    private final List<Slot> slots = new ArrayList<>();
    private final Set<String> hidden = new LinkedHashSet<>();
    private final Map<String, Object[]> folderIcons = new HashMap<>();
    private final Map<String, HomeItem> itemCache = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable saveRunnable = this::persistNow;
    private Map<String, AppInfo> byKey = new HashMap<>();
    private boolean dirty;
    private int page0Cap = -1;
    private boolean extraPage;

    HomeLayout(@NonNull Context context) {
        Context app = context.getApplicationContext();
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        appContext = app;
        allAppsOnHome = DeviceLayout.mode(app) == DeviceLayout.Mode.ALL_APPS_ON_HOME;
        iconPx = DeviceLayout.iconPx(app);
        AppRepository.initMask();
        blockBlast = new AppInfo(BLOCK_BLAST_COMPONENT, app.getString(R.string.block_blast_name), AppRepository.toBitmap(androidx.core.content.res.ResourcesCompat.getDrawable(app.getResources(), R.drawable.ic_block_blast, null), iconPx));
        for (int i = 0; i < QuizGames.ALL.length; i++) {
            QuizGames.QuizGameItem g = QuizGames.ALL[i];
            Bitmap gameIcon = AppRepository.toBitmap(androidx.core.content.res.ResourcesCompat.getDrawable(app.getResources(), g.icon, null), iconPx);
            drawAdBadge(gameIcon);
            quizApps.add(new AppInfo(QuizGames.component(i), g.name, gameIcon));
        }
        load();
        if (prefs.getString(KEY, null) != null && loadedVersion < LAYOUT_VERSION) {
            slots.clear();
            hidden.clear();
            save();
        }
    }

    private static void drawAdBadge(@NonNull Bitmap icon) {
        float s = icon.getWidth();
        float w = s * 0.30f;
        float h = s * 0.19f;
        float left = s * 0.03f;
        float top = s * 0.03f;
        RectF r = new RectF(left, top, left + w, top + h);
        Canvas c = new Canvas(icon);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(0xFFEDB000);
        c.drawRoundRect(r, h * 0.3f, h * 0.3f, bg);
        Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        txt.setColor(0xFFFFFFFF);
        txt.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setTextSize(h * 0.68f);
        float baseline = r.centerY() - (txt.descent() + txt.ascent()) / 2f;
        c.drawText("AD", r.centerX(), baseline, txt);
    }

    @NonNull
    List<AppInfo> quizGames() {
        return quizApps;
    }

    AppInfo blockBlastItem() {
        return blockBlast;
    }

    static String key(@NonNull AppInfo a) {
        return a.component.flattenToShortString();
    }

    private void load() {
        String json = prefs.getString(KEY, null);
        if (json == null) return;
        try {
            JSONObject o = new JSONObject(json);
            loadedVersion = o.optInt("lv", 0);
            JSONArray h = o.optJSONArray("hidden");
            if (h != null) for (int i = 0; i < h.length(); i++) hidden.add(h.getString(i));
            if (o.optInt("v", 1) >= 2) {
                JSONArray arr = o.getJSONArray("slots");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject e = arr.getJSONObject(i);
                    slots.add(new Slot(readEntry(e), e.getInt("p"), e.getInt("c")));
                }
            } else {
                JSONArray arr = o.optJSONArray("order");
                int home = o.optInt("h", 0);
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        Object e = readEntry(arr.getJSONObject(i));
                        if (i < home) {
                            slots.add(new Slot(e, 0, i));
                        } else {
                            int n = i - home;
                            slots.add(new Slot(e, 1 + n / PER_PAGE, n % PER_PAGE));
                        }
                    }
                }
                dirty = true;
                handler.postDelayed(saveRunnable, SAVE_DELAY_MS);
            }
        } catch (JSONException e) {
            slots.clear();
            hidden.clear();
        }
    }

    private static Object readEntry(JSONObject e) throws JSONException {
        if (e.has("a")) return e.getString("a");
        Folder f = new Folder(e.getString("f"), e.optString("n", DEFAULT_FOLDER_NAME));
        JSONArray fa = e.getJSONArray("apps");
        for (int j = 0; j < fa.length(); j++) f.apps.add(fa.getString(j));
        return f;
    }

    private void save() {
        dirty = true;
        handler.removeCallbacks(saveRunnable);
        handler.postDelayed(saveRunnable, SAVE_DELAY_MS);
    }

    void flush() {
        handler.removeCallbacks(saveRunnable);
        persistNow();
    }

    private void persistNow() {
        if (!dirty) return;
        dirty = false;
        final List<Slot> copy = new ArrayList<>(slots.size());
        for (Slot s : slots) {
            Object e = s.e;
            if (e instanceof Folder) {
                Folder f = (Folder) e;
                Folder c = new Folder(f.id, f.name);
                c.apps.addAll(f.apps);
                e = c;
            }
            copy.add(new Slot(e, s.page, s.cell));
        }
        final List<String> hiddenCopy = new ArrayList<>(hidden);
        IO.execute(() -> write(copy, hiddenCopy));
    }

    private void write(List<Slot> entries, List<String> hiddenKeys) {
        try {
            JSONArray arr = new JSONArray();
            for (Slot s : entries) {
                JSONObject j = new JSONObject();
                if (s.e instanceof String) {
                    j.put("a", s.e);
                } else {
                    Folder f = (Folder) s.e;
                    j.put("f", f.id);
                    j.put("n", f.name);
                    j.put("apps", new JSONArray(f.apps));
                }
                j.put("p", s.page);
                j.put("c", s.cell);
                arr.put(j);
            }
            JSONObject o = new JSONObject();
            o.put("v", 2);
            o.put("lv", LAYOUT_VERSION);
            o.put("slots", arr);
            o.put("hidden", new JSONArray(hiddenKeys));
            prefs.edit().putString(KEY, o.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    private int capOf(int page) {
        if (page == DOCK) return DOCK_SLOTS;
        return page == 0 ? page0Cap : PER_PAGE;
    }

    private boolean validCell(int page, int cell) {
        if (page == DOCK) return cell >= 0 && cell < DOCK_SLOTS;
        if (page < 0 || cell < 0) return false;
        int cap = capOf(page);
        return cap < 0 || cell < cap;
    }

    @Nullable
    private Slot slotAt(int page, int cell) {
        for (Slot s : slots) if (s.page == page && s.cell == cell) return s;
        return null;
    }

    @Nullable
    private Slot slotOf(String itemKey) {
        for (Slot s : slots) if (s.itemKey().equals(itemKey)) return s;
        return null;
    }

    private int lastUsedPage() {
        int m = 0;
        for (Slot s : slots) m = Math.max(m, s.page);
        return m;
    }

    int pageCount() {
        return lastUsedPage() + 1 + (extraPage ? 1 : 0);
    }

    int compactPages(int currentPage) {
        if (extraPage) return currentPage;
        java.util.TreeSet<Integer> used = new java.util.TreeSet<>();
        for (Slot s : slots) if (s.page > 0) used.add(s.page);
        boolean gap = false;
        int expect = 1;
        for (int p : used) {
            if (p != expect) gap = true;
            expect++;
        }
        if (!gap) return currentPage;
        Map<Integer, Integer> map = new HashMap<>();
        int n = 1;
        for (int p : used) map.put(p, n++);
        int newCurrent = 0;
        for (int p : used) if (p <= currentPage) newCurrent = map.get(p);
        if (currentPage == 0) newCurrent = 0;
        for (Slot s : slots) if (s.page > 0) s.page = map.get(s.page);
        itemCache.clear();
        save();
        return newCurrent;
    }

    boolean lastPageHasItems() {
        int last = lastUsedPage();
        for (Slot s : slots) if (s.page == last) return true;
        return false;
    }

    void setExtraPage(boolean on) {
        extraPage = on;
    }

    boolean hasExtraPage() {
        return extraPage;
    }

    private int[] firstFree() {
        int maxPage = lastUsedPage();
        for (int p = 1; p <= maxPage + 1; p++) {
            int cap = capOf(p);
            if (cap < 0) cap = 0;
            for (int c = 0; c < cap; c++) if (slotAt(p, c) == null) return new int[]{p, c};
        }
        return new int[]{Math.max(1, maxPage + 1), 0};
    }

    private int[] nearestFree(int page, int cell) {
        int cap = Math.max(0, capOf(page));
        int best = -1;
        int bestD = Integer.MAX_VALUE;
        for (int c = 0; c < cap; c++) {
            if (slotAt(page, c) != null) continue;
            int d = Math.abs(c / COLUMNS - cell / COLUMNS) * 10 + Math.abs(c % COLUMNS - cell % COLUMNS);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        if (best >= 0) return new int[]{page, best};
        return firstFree();
    }

    boolean setPage0Capacity(int cap) {
        cap = Math.max(0, cap);
        if (cap == page0Cap) return false;
        page0Cap = cap;
        boolean moved = false;
        for (Slot s : new ArrayList<>(slots)) {
            if (s.page == 0 && s.cell >= cap) {
                int[] f = firstFree();
                s.page = f[0];
                s.cell = f[1];
                moved = true;
            }
        }
        if (moved) {
            itemCache.clear();
            save();
        }
        return moved;
    }

    void build(@NonNull List<AppInfo> apps) {
        Map<String, AppInfo> map = new HashMap<>();
        for (AppInfo a : apps) map.put(key(a), a);
        map.put(key(blockBlast), blockBlast);
        for (AppInfo q : quizApps) map.put(key(q), q);
        byKey = map;

        boolean changed = false;
        if (!apps.isEmpty()) {
            if (initDockIfNeeded(apps)) changed = true;
            if (syncManagedFolders(apps)) changed = true;
            Set<String> placed = new HashSet<>();
            Set<Integer> used = new HashSet<>();
            for (int i = 0; i < slots.size(); ) {
                Slot s = slots.get(i);
                boolean drop = false;
                if (s.e instanceof String) {
                    String k = (String) s.e;
                    drop = !map.containsKey(k) || hidden.contains(k) || !placed.add(k);
                } else {
                    Folder f = (Folder) s.e;
                    final boolean mirror = GOOGLE_HOME_ID.equals(f.id) || SOCIAL_HOME_ID.equals(f.id);
                    for (int j = 0; j < f.apps.size(); ) {
                        String k = f.apps.get(j);
                        if (!map.containsKey(k) || (!mirror && ((hidden.contains(k) && !isManagedFolder(f)) || !placed.add(k)))) {
                            f.apps.remove(j);
                            changed = true;
                        } else {
                            j++;
                        }
                    }
                    if (f.apps.isEmpty()) {
                        drop = true;
                    } else if (f.apps.size() == 1 && !isManagedFolder(f)) {
                        s.e = f.apps.get(0);
                        changed = true;
                    }
                }
                if (drop) {
                    slots.remove(i);
                    changed = true;
                    continue;
                }
                if (!used.add(s.page * 1000 + s.cell)) {
                    int[] f = firstFree();
                    s.page = f[0];
                    s.cell = f[1];
                    used.add(s.page * 1000 + s.cell);
                    changed = true;
                }
                i++;
            }
            for (AppInfo a : allAppsOnHome ? apps : java.util.Collections.<AppInfo>emptyList()) {
                String k = key(a);
                if (placed.contains(k) || hidden.contains(k)) continue;
                int[] f = firstFree();
                slots.add(new Slot(k, f[0], f[1]));
                placed.add(k);
                changed = true;
            }
            if (changed) save();
        }
        itemCache.clear();
        Set<String> liveFolders = new HashSet<>();
        for (Slot s : slots) if (s.e instanceof Folder) liveFolders.add(((Folder) s.e).id);
        folderIcons.keySet().retainAll(liveFolders);
    }

    static final String GOOGLE_FOLDER_ID = "google";
    static final String GOOGLE_FOLDER_NAME = "Google";
    private static final String[] GOOGLE_PACKAGES = {"com.google.android.googlequicksearchbox", "com.google.android.gm", "com.google.android.apps.maps", "com.google.android.youtube", "com.google.android.apps.docs", "com.google.android.apps.photos", "com.android.chrome", "com.google.android.calendar", "com.google.android.apps.tachyon", "com.google.android.videos", "com.android.vending", "com.google.android.apps.subscriptions.red", "com.google.android.keep", "com.google.android.apps.walletnfcrel", "com.google.android.apps.translate", "com.google.android.apps.nbu.files", "com.google.android.apps.bard", "com.google.android.apps.messaging", "com.google.android.contacts", "com.google.android.dialer", "com.google.android.apps.docs.editors.docs", "com.google.android.apps.docs.editors.sheets", "com.google.android.apps.docs.editors.slides", "com.google.android.apps.youtube.music", "com.google.android.apps.podcasts", "com.google.earth", "com.google.ar.lens",};

    static boolean isGooglePackage(@NonNull String p) {
        for (String g : GOOGLE_PACKAGES) if (g.equals(p)) return true;
        if (p.startsWith("com.google.zxing") || p.startsWith("com.google.mlkit")) return false;
        if (p.equals("com.google.android.deskclock") || p.equals("com.google.android.calculator"))
            return false;
        return p.startsWith("com.google.");
    }

    private static int googleOrder(String pkg) {
        for (int i = 0; i < GOOGLE_PACKAGES.length; i++)
            if (GOOGLE_PACKAGES[i].equals(pkg)) return i;
        return GOOGLE_PACKAGES.length;
    }

    static final String SOCIAL_FOLDER_ID = "social";
    static final String SOCIAL_FOLDER_NAME = "Social";
    static final String GOOGLE_HOME_ID = "google_home";
    static final String SOCIAL_HOME_ID = "social_home";
    static final String TOOLS_FOLDER_ID = "tools";
    static final String TOOLS_FOLDER_NAME = "Tools";
    static final String MORE_GAMES_FOLDER_ID = "moregames";
    static final String MORE_GAMES_FOLDER_NAME = "More Games";
    private static final String[] SOCIAL_PACKAGES = {"com.whatsapp", "com.whatsapp.w4b", "com.facebook.katana", "com.instagram.android", "com.snapchat.android", "org.telegram.messenger", "org.telegram.messenger.web", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",};

    static boolean isSocialPackage(@NonNull String p) {
        for (String s : SOCIAL_PACKAGES) if (s.equals(p)) return true;
        return false;
    }

    private static int socialOrder(String pkg) {
        for (int i = 0; i < SOCIAL_PACKAGES.length; i++)
            if (SOCIAL_PACKAGES[i].equals(pkg)) return i;
        return SOCIAL_PACKAGES.length;
    }

    private static boolean isManagedFolder(Folder f) {
        return GOOGLE_FOLDER_ID.equals(f.id) || SOCIAL_FOLDER_ID.equals(f.id) || MORE_GAMES_FOLDER_ID.equals(f.id) || TOOLS_FOLDER_ID.equals(f.id) || GOOGLE_HOME_ID.equals(f.id) || SOCIAL_HOME_ID.equals(f.id);
    }

    private boolean syncManagedFolders(@NonNull List<AppInfo> apps) {
        List<AppInfo> tools = ToolsApps.resolve(appContext, apps);
        Set<String> toolKeys = new HashSet<>();
        for (AppInfo t : tools) toolKeys.add(key(t));
        List<AppInfo> google = new ArrayList<>();
        List<AppInfo> social = new ArrayList<>();
        for (AppInfo a : apps) {
            String p = a.component.getPackageName();
            if (toolKeys.contains(key(a))) continue;
            if (isGooglePackage(p)) google.add(a);
            else if (isSocialPackage(p)) social.add(a);
        }
        google.sort((x, y) -> {
            int c = Integer.compare(googleOrder(x.component.getPackageName()), googleOrder(y.component.getPackageName()));
            return c != 0 ? c : x.label.compareToIgnoreCase(y.label);
        });
        social.sort((x, y) -> {
            int c = Integer.compare(socialOrder(x.component.getPackageName()), socialOrder(y.component.getPackageName()));
            return c != 0 ? c : x.label.compareToIgnoreCase(y.label);
        });
        final Set<String> leftKeys = prefs.getStringSet(PREF_FOLDER_LEFT, java.util.Collections.emptySet());
        final Set<String> inDock = dockKeys();
        boolean socialAvailable = false;
        for (AppInfo s : social)
            if (!leftKeys.contains(key(s)) && !inDock.contains(key(s))) socialAvailable = true;
        boolean shifted = false;
        Slot toolsSlot = slotOf("folder:" + TOOLS_FOLDER_ID);
        if (toolsSlot != null && toolsSlot.page == 1) {
            if (socialAvailable && slotOf("folder:" + SOCIAL_FOLDER_ID) == null && toolsSlot.cell == 1 && slotAt(1, 2) == null) {
                toolsSlot.cell = 2;
                shifted = true;
            } else if (!socialAvailable && slotOf("folder:" + SOCIAL_FOLDER_ID) == null && toolsSlot.cell == 2 && slotAt(1, 1) == null) {
                toolsSlot.cell = 1;
                shifted = true;
            }
            if (shifted) itemCache.remove("folder:" + TOOLS_FOLDER_ID);
        }
        boolean t = syncManagedFolder(tools, TOOLS_FOLDER_ID, TOOLS_FOLDER_NAME, null, 1, socialAvailable ? 2 : 1);
        boolean a = syncManagedFolder(google, GOOGLE_FOLDER_ID, GOOGLE_FOLDER_NAME, null, 1, 0, GOOGLE_HOME_ID, page0Cap - 4);
        boolean b = syncManagedFolder(social, SOCIAL_FOLDER_ID, SOCIAL_FOLDER_NAME, null, 1, 1, SOCIAL_HOME_ID, page0Cap - 3);
        t |= shifted;
        boolean c = pinBlockBlast();
        boolean d = syncManagedFolder(quizApps, MORE_GAMES_FOLDER_ID, MORE_GAMES_FOLDER_NAME, "app:" + key(blockBlast), -1, -1);
        return t || a || b || c || d;
    }

    private boolean syncManagedFolder(@NonNull List<AppInfo> members, @NonNull String id, @NonNull String name, @Nullable String besideId, int prefPage, int prefCell) {
        return syncManagedFolder(members, id, name, besideId, prefPage, prefCell, null, -1);
    }

    private boolean syncManagedFolder(@NonNull List<AppInfo> members, @NonNull String id, @NonNull String name, @Nullable String besideId, int prefPage, int prefCell, @Nullable String mirrorId, int mirrorCell) {
        name = prefs.getString("custom_name_" + id, name);
        final Set<String> left = prefs.getStringSet(PREF_FOLDER_LEFT, java.util.Collections.emptySet());
        List<String> keys = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        final Set<String> inDock = dockKeys();
        for (AppInfo a : members) {
            if (left.contains(key(a)) || inDock.contains(key(a))) continue;
            if (seen.add(a.component.getPackageName() + "/" + a.label)) keys.add(key(a));
        }

        Slot fs = slotOf("folder:" + id);
        Slot ms = mirrorId == null ? null : slotOf("folder:" + mirrorId);
        boolean changed = false;
        Set<String> keySet = new HashSet<>(keys);
        for (int i = slots.size() - 1; i >= 0; i--) {
            Slot s = slots.get(i);
            if (s.e instanceof String && keySet.contains(s.e)) {
                slots.remove(i);
                changed = true;
            } else if (s.e instanceof Folder && s != fs && s != ms) {
                Folder f = (Folder) s.e;
                if (f.apps.removeAll(keySet)) {
                    itemCache.remove(s.itemKey());
                    changed = true;
                }
            }
        }
        if (keys.isEmpty()) {
            if (fs != null) {
                slots.remove(fs);
                itemCache.remove("folder:" + id);
                changed = true;
            }
            if (ms != null) {
                slots.remove(ms);
                itemCache.remove("folder:" + mirrorId);
                changed = true;
            }
            return changed;
        }
        if (fs == null) {
            if (page0Cap < 4) return changed;
            Folder f = new Folder(id, name);
            f.apps.addAll(keys);
            int[] spot = besideId != null ? folderSpot(besideId) : prefPage >= 0 ? spotNear(prefPage, prefCell) : folderSpot(null);
            slots.add(new Slot(f, spot[0], spot[1]));
            itemCache.clear();
            changed = true;
        } else {
            Folder gf = (Folder) fs.e;
            boolean c = false;
            if (!gf.name.equals(name)) {
                gf.name = name;
                c = true;
            }
            if (!gf.apps.equals(keys)) {
                gf.apps.clear();
                gf.apps.addAll(keys);
                c = true;
            }
            if (c) {
                itemCache.remove("folder:" + id);
                changed = true;
            }
        }
        if (mirrorId != null && page0Cap >= 4) {
            if (ms == null) {
                Folder m = new Folder(mirrorId, name);
                m.apps.addAll(keys);
                int[] spot = spotNear(0, mirrorCell);
                slots.add(new Slot(m, spot[0], spot[1]));
                itemCache.clear();
                changed = true;
            } else {
                Folder m = (Folder) ms.e;
                boolean c = false;
                if (!m.name.equals(name)) {
                    m.name = name;
                    c = true;
                }
                if (!m.apps.equals(keys)) {
                    m.apps.clear();
                    m.apps.addAll(keys);
                    c = true;
                }
                if (c) {
                    itemCache.remove("folder:" + mirrorId);
                    changed = true;
                }
            }
        }
        return changed;
    }

    static final ComponentName BLOCK_BLAST_COMPONENT = new ComponentName("com.customapps.launchersdk", "dedicated.BlockBlast");

    static boolean isBlockBlast(@NonNull AppInfo a) {
        return BLOCK_BLAST_COMPONENT.equals(a.component);
    }

    private boolean pinBlockBlast() {
        if (page0Cap < 4) return false;
        String k = key(blockBlast);
        if (hidden.contains(k) || slotOf("app:" + k) != null) return false;
        for (Slot s : slots)
            if (s.e instanceof Folder && ((Folder) s.e).apps.contains(k)) return false;
        int[] spot = folderSpot(null);
        slots.add(new Slot(k, spot[0], spot[1]));
        itemCache.clear();
        return true;
    }

    private int[] spotNear(int page, int cell) {
        if (validCell(page, cell) && slotAt(page, cell) == null) return new int[]{page, cell};
        return nearestFree(page, cell);
    }

    private int[] folderSpot(@Nullable String besideItemKey) {
        Slot anchor = besideItemKey == null ? null : slotOf(besideItemKey);
        if (anchor != null) {
            int col = anchor.cell % COLUMNS;
            int[] order = {anchor.cell + 1, anchor.cell - 1};
            for (int c : order) {
                int ccol = c % COLUMNS;
                boolean sameRow = c >= 0 && Math.abs(ccol - col) == 1 && c / COLUMNS == anchor.cell / COLUMNS;
                if (sameRow && validCell(anchor.page, c) && slotAt(anchor.page, c) == null)
                    return new int[]{anchor.page, c};
            }
            return nearestFree(anchor.page, anchor.cell);
        }
        for (int c = page0Cap - 4; c < page0Cap; c++)
            if (slotAt(0, c) == null) return new int[]{0, c};
        for (int c = page0Cap - 1; c >= 0; c--) if (slotAt(0, c) == null) return new int[]{0, c};
        return firstFree();
    }

    @NonNull
    List<HomeItem[]> pages() {
        int count = pageCount();
        List<HomeItem[]> out = new ArrayList<>(count);
        for (int p = 0; p < count; p++)
            out.add(new HomeItem[Math.max(0, p == 0 ? page0Cap : PER_PAGE)]);
        for (Slot s : slots) {
            if (s.page < 0) continue;
            HomeItem[] page = out.get(s.page);
            if (s.cell < 0 || s.cell >= page.length) continue;
            HomeItem it = itemFor(s);
            if (it == null) continue;
            page[s.cell] = it;
        }
        return out;
    }

    @Nullable
    private HomeItem itemFor(Slot s) {
        HomeItem it = itemCache.get(s.itemKey());
        if (it == null) {
            if (s.e instanceof String) {
                AppInfo a = byKey.get(s.e);
                if (a == null) return null;
                it = HomeItem.ofApp(a);
            } else {
                it = folderItem((Folder) s.e);
            }
            itemCache.put(s.itemKey(), it);
        }
        return it;
    }

    @NonNull
    HomeItem[] dockItems() {
        HomeItem[] out = new HomeItem[DOCK_SLOTS];
        for (Slot s : slots)
            if (s.page == DOCK && s.cell >= 0 && s.cell < DOCK_SLOTS) out[s.cell] = itemFor(s);
        return out;
    }

    @Nullable
    HomeItem dockItem(int cell) {
        Slot s = slotAt(DOCK, cell);
        return s == null ? null : itemFor(s);
    }

    boolean setDockDefaults(@NonNull List<DockItem> items) {
        if (prefs.getBoolean(PREF_DOCK_INIT, false)) return false;
        dockDefaults = new ArrayList<>(items);
        return true;
    }

    static boolean dockEligible(@NonNull HomeItem it) {
        return it.app != null || it.folderId != null;
    }

    static boolean dockAccepts(@NonNull HomeItem dragged, @Nullable HomeItem target) {
        return dockEligible(dragged) && (target == null || dockEligible(target));
    }

    private boolean slotDockEligible() {
        return true;
    }

    private Set<String> dockKeys() {
        Set<String> out = new HashSet<>();
        for (Slot s : slots) {
            if (s.page != DOCK) continue;
            if (s.e instanceof String) out.add((String) s.e);
            else if (!isManagedFolder((Folder) s.e)) out.addAll(((Folder) s.e).apps);
        }
        return out;
    }

    private boolean initDockIfNeeded(@NonNull List<AppInfo> apps) {
        if (dockDefaults == null || prefs.getBoolean(PREF_DOCK_INIT, false)) return false;
        for (Slot s : slots) {
            if (s.page == DOCK) {
                prefs.edit().putBoolean(PREF_DOCK_INIT, true).apply();
                dockDefaults = null;
                return false;
            }
        }
        boolean placed = false;
        for (int i = 0; i < DOCK_SLOTS && i < dockDefaults.size(); i++) {
            DockItem d = dockDefaults.get(i);
            ComponentName c = d == null ? null : d.intent.getComponent();
            if (c == null) continue;
            AppInfo match = null;
            for (AppInfo a : apps) {
                if (a.component.equals(c)) {
                    match = a;
                    break;
                }
                if (match == null && a.component.getPackageName().equals(c.getPackageName()))
                    match = a;
            }
            if (match == null) continue;
            String k = key(match);
            if (hidden.contains(k) || dockKeys().contains(k)) continue;
            for (int j = slots.size() - 1; j >= 0; j--) {
                Slot s = slots.get(j);
                if (s.e instanceof String && s.e.equals(k)) {
                    slots.remove(j);
                } else if (s.e instanceof Folder && ((Folder) s.e).apps.remove(k)) {
                    itemCache.remove(s.itemKey());
                }
            }
            slots.add(new Slot(k, DOCK, i));
            placed = true;
        }
        if (placed) {
            prefs.edit().putBoolean(PREF_DOCK_INIT, true).apply();
            dockDefaults = null;
            itemCache.clear();
        }
        return placed;
    }

    private void rememberLeft(java.util.Collection<String> keys) {
        Set<String> left = new HashSet<>(prefs.getStringSet(PREF_FOLDER_LEFT, java.util.Collections.emptySet()));
        if (left.addAll(keys)) prefs.edit().putStringSet(PREF_FOLDER_LEFT, left).apply();
    }

    private HomeItem folderItem(Folder f) {
        List<AppInfo> list = folderApps(f);
        StringBuilder sig = new StringBuilder();
        for (AppInfo a : list)
            sig.append(key(a)).append('#').append(System.identityHashCode(a.icon)).append(';');
        Object[] cached = folderIcons.get(f.id);
        Bitmap icon;
        if (cached != null && cached[0].equals(sig.toString())) {
            icon = (Bitmap) cached[1];
        } else {
            icon = renderFolder(list);
            folderIcons.put(f.id, new Object[]{sig.toString(), icon});
        }
        return HomeItem.ofFolder(f.id, f.name, icon, list);
    }

    private Bitmap renderFolder(List<AppInfo> list) {
        int size = iconPx;
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        android.graphics.Path shape = new android.graphics.Path();
        IconShape.fit(shape, 0, 0, size);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(FOLDER_BG_COLOR);
        c.drawPath(shape, bg);
        final int g = 3;
        int n = Math.min(list.size(), g * g);
        float pad = size * 0.13f;
        float gap = size * 0.06f;
        float cell = (size - 2 * pad - (g - 1) * gap) / g;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));
        android.graphics.Path miniMask = new android.graphics.Path();
        for (int i = 0; i < n; i++) {
            int col = i % g;
            int row = i / g;
            float l = pad + col * (cell + gap);
            float t = pad + row * (cell + gap);
            RectF dst = new RectF(l, t, l + cell, t + cell);
            int layer = c.saveLayer(dst, null);
            c.drawBitmap(list.get(i).icon, null, dst, p);
            IconShape.fit(miniMask, l, t, cell);
            c.drawPath(miniMask, maskPaint);
            c.restoreToCount(layer);
        }
        b.prepareToDraw();
        return b;
    }

    private List<AppInfo> folderApps(Folder f) {
        List<AppInfo> list = new ArrayList<>(f.apps.size());
        for (String k : f.apps) {
            AppInfo a = byKey.get(k);
            if (a != null) list.add(a);
        }
        return list;
    }

    @Nullable
    Folder getFolder(@Nullable String id) {
        if (id == null) return null;
        for (Slot s : slots)
            if (s.e instanceof Folder && ((Folder) s.e).id.equals(id)) return (Folder) s.e;
        return null;
    }

    @NonNull
    List<AppInfo> folderApps(@Nullable String id) {
        Folder f = getFolder(id);
        return f == null ? new ArrayList<>() : folderApps(f);
    }

    boolean moveTo(String itemKey, int page, int cell) {
        Slot s = slotOf(itemKey);
        if (s == null || !validCell(page, cell)) return false;
        if (s.page == page && s.cell == cell) return false;
        Slot target = slotAt(page, cell);
        if (page == DOCK && !slotDockEligible()) return false;
        if (target != null && s.page == DOCK && !slotDockEligible()) return false;
        if (target != null) {
            target.page = s.page;
            target.cell = s.cell;
        }
        s.page = page;
        s.cell = cell;
        save();
        return true;
    }

    @Nullable
    String createFolder(String dragKey, String targetKey) {
        Slot d = slotOf(dragKey);
        Slot t = slotOf(targetKey);
        if (d == null || t == null || d == t || !(d.e instanceof String)) return null;
        String dragged = (String) d.e;
        String resultKey;
        if (t.e instanceof Folder) {
            ((Folder) t.e).apps.add(dragged);
            resultKey = targetKey;
        } else {
            Folder f = new Folder(Long.toHexString(System.nanoTime()), DEFAULT_FOLDER_NAME);
            f.apps.add((String) t.e);
            f.apps.add(dragged);
            t.e = f;
            resultKey = "folder:" + f.id;
        }
        slots.remove(d);
        itemCache.remove(dragKey);
        itemCache.remove(targetKey);
        save();
        return resultKey;
    }

    boolean removeFromFolder(String folderId, String appKey, int prefPage, int prefCell) {
        Slot fs = slotOf("folder:" + folderId);
        if (fs == null) return false;
        Folder f = (Folder) fs.e;
        if (!f.apps.remove(appKey)) return false;
        itemCache.remove("folder:" + folderId);
        if (isManagedFolderId(folderId)) {
            Set<String> left = new HashSet<>(prefs.getStringSet(PREF_FOLDER_LEFT, java.util.Collections.emptySet()));
            if (left.add(appKey)) prefs.edit().putStringSet(PREF_FOLDER_LEFT, left).apply();
        }
        int[] dest;
        if (prefCell >= 0 && validCell(prefPage, prefCell) && slotAt(prefPage, prefCell) == null) {
            dest = new int[]{prefPage, prefCell};
        } else {
            dest = nearestFree(fs.page, fs.cell);
        }
        if (f.apps.size() == 1) fs.e = f.apps.get(0);
        else if (f.apps.isEmpty()) slots.remove(fs);
        slots.add(new Slot(appKey, dest[0], dest[1]));
        save();
        return true;
    }

    boolean removeFolder(String folderId) {
        Slot fs = slotOf("folder:" + folderId);
        if (fs == null || !(fs.e instanceof Folder)) return false;
        Folder f = (Folder) fs.e;
        itemCache.remove("folder:" + folderId);
        slots.remove(fs);
        hidden.addAll(f.apps);
        if (fs.page == DOCK) rememberLeft(f.apps);
        if (isManagedFolderId(folderId)) {
            Set<String> left = new HashSet<>(prefs.getStringSet(PREF_FOLDER_LEFT, java.util.Collections.emptySet()));
            if (left.addAll(f.apps)) prefs.edit().putStringSet(PREF_FOLDER_LEFT, left).apply();
        }
        save();
        return true;
    }

    boolean removeFromFolderOnly(String folderId, String appKey) {
        Slot fs = slotOf("folder:" + folderId);
        if (fs == null || !(fs.e instanceof Folder)) return false;
        Folder f = (Folder) fs.e;
        if (!f.apps.remove(appKey)) return false;
        itemCache.remove("folder:" + folderId);
        hidden.add(appKey);
        if (fs.page == DOCK) rememberLeft(java.util.Collections.singleton(appKey));
        if (isManagedFolderId(folderId)) {
            Set<String> left = new HashSet<>(prefs.getStringSet(PREF_FOLDER_LEFT, java.util.Collections.emptySet()));
            if (left.add(appKey)) prefs.edit().putStringSet(PREF_FOLDER_LEFT, left).apply();
        }
        if (f.apps.isEmpty()) {
            slots.remove(fs);
        } else if (f.apps.size() == 1) {
            fs.e = f.apps.get(0);
            itemCache.remove(fs.itemKey());
        }
        save();
        return true;
    }

    @Nullable
    String moveFromFolder(String folderId, String appKey, String targetKey) {
        Slot fs = slotOf("folder:" + folderId);
        Slot t = slotOf(targetKey);
        if (fs == null || t == null || fs == t || !(fs.e instanceof Folder)) return null;
        Folder src = (Folder) fs.e;
        if (!src.apps.remove(appKey)) return null;
        itemCache.remove("folder:" + folderId);
        itemCache.remove(targetKey);
        String resultKey;
        if (t.e instanceof Folder) {
            ((Folder) t.e).apps.add(appKey);
            resultKey = targetKey;
        } else {
            Folder f = new Folder(Long.toHexString(System.nanoTime()), DEFAULT_FOLDER_NAME);
            f.apps.add((String) t.e);
            f.apps.add(appKey);
            t.e = f;
            resultKey = "folder:" + f.id;
        }
        if (src.apps.size() == 1) fs.e = src.apps.get(0);
        else if (src.apps.isEmpty()) slots.remove(fs);
        save();
        return resultKey;
    }

    @Nullable
    String folderIdOf(@NonNull AppInfo app) {
        String k = key(app);
        for (Slot s : slots)
            if (s.e instanceof Folder && ((Folder) s.e).apps.contains(k)) return ((Folder) s.e).id;
        return null;
    }

    @Nullable
    String placeFromDrawer(@NonNull AppInfo app, int page, int cell, @Nullable String targetKey) {
        String k = key(app);
        String itemKey = "app:" + k;
        String folderId = folderIdOf(app);
        Slot own = slotOf(itemKey);
        if (targetKey != null) {
            Slot t = slotOf(targetKey);
            if (t == null || targetKey.equals(itemKey)) return null;
            if (folderId != null) return moveFromFolder(folderId, k, targetKey);
            if (own != null) return createFolder(itemKey, targetKey);
            hidden.remove(k);
            itemCache.remove(targetKey);
            if (t.e instanceof Folder) {
                ((Folder) t.e).apps.add(k);
                save();
                return targetKey;
            }
            Folder f = new Folder(Long.toHexString(System.nanoTime()), DEFAULT_FOLDER_NAME);
            f.apps.add((String) t.e);
            f.apps.add(k);
            t.e = f;
            save();
            return "folder:" + f.id;
        }
        if (!validCell(page, cell) || slotAt(page, cell) != null) return null;
        if (folderId != null) return removeFromFolder(folderId, k, page, cell) ? itemKey : null;
        if (own != null) return moveTo(itemKey, page, cell) ? itemKey : null;
        hidden.remove(k);
        itemCache.remove(itemKey);
        slots.add(new Slot(k, page, cell));
        save();
        return itemKey;
    }

    boolean reorderInFolder(String folderId, int from, int to) {
        Folder f = getFolder(folderId);
        if (f == null || from < 0 || to < 0 || from >= f.apps.size() || to >= f.apps.size() || from == to)
            return false;
        f.apps.add(to, f.apps.remove(from));
        itemCache.remove("folder:" + folderId);
        save();
        return true;
    }

    boolean renameFolder(String folderId, String name) {
        Folder f = getFolder(folderId);
        if (f == null) return false;
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) n = DEFAULT_FOLDER_NAME;
        if (n.equals(f.name)) return false;
        f.name = n;
        itemCache.remove("folder:" + folderId);
        String base = GOOGLE_HOME_ID.equals(folderId) ? GOOGLE_FOLDER_ID : SOCIAL_HOME_ID.equals(folderId) ? SOCIAL_FOLDER_ID : folderId;
        String other = GOOGLE_FOLDER_ID.equals(base) ? GOOGLE_HOME_ID : SOCIAL_FOLDER_ID.equals(base) ? SOCIAL_HOME_ID : null;
        if (GOOGLE_FOLDER_ID.equals(base) || SOCIAL_FOLDER_ID.equals(base) || TOOLS_FOLDER_ID.equals(base) || MORE_GAMES_FOLDER_ID.equals(base)) {
            prefs.edit().putString("custom_name_" + base, n).apply();
        }
        for (String id : new String[]{base, other}) {
            Folder o = id == null || id.equals(folderId) ? null : getFolder(id);
            if (o != null) {
                o.name = n;
                itemCache.remove("folder:" + id);
            }
        }
        save();
        return true;
    }

    boolean hide(AppInfo app) {
        String k = key(app);
        boolean changed = hidden.add(k);
        Slot s = slotOf("app:" + k);
        if (s != null) {
            if (s.page == DOCK) rememberLeft(java.util.Collections.singleton(k));
            slots.remove(s);
            changed = true;
        }
        itemCache.remove("app:" + k);
        if (changed) save();
        return changed;
    }

    private static final String PREF_FOLDER_LEFT = "folder_left_keys";

    static boolean isManagedFolderId(@Nullable String id) {
        return GOOGLE_FOLDER_ID.equals(id) || SOCIAL_FOLDER_ID.equals(id) || MORE_GAMES_FOLDER_ID.equals(id) || TOOLS_FOLDER_ID.equals(id) || GOOGLE_HOME_ID.equals(id) || SOCIAL_HOME_ID.equals(id);
    }

    boolean canAddToHome(@NonNull AppInfo app) {
        String k = key(app);
        if (slotOf("app:" + k) != null) return false;
        for (Slot s : slots)
            if (s.e instanceof Folder && ((Folder) s.e).apps.contains(k)) return false;
        return true;
    }

    boolean addToHome(@NonNull AppInfo app) {
        if (!canAddToHome(app)) return false;
        String k = key(app);
        int page = Math.max(1, lastUsedPage());
        int last = -1;
        for (Slot s : slots) if (s.page == page && s.cell > last) last = s.cell;
        int cell = last + 1;
        if (cell >= capOf(page)) {
            page++;
            cell = 0;
        }
        hidden.remove(k);
        itemCache.remove("app:" + k);
        slots.add(new Slot(k, page, cell));
        save();
        return true;
    }
}