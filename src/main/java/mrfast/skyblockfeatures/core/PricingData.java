package mrfast.skyblockfeatures.core;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;
import org.lwjgl.Sys;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import mrfast.skyblockfeatures.SkyblockFeatures;
import mrfast.skyblockfeatures.utils.APIUtils;
import mrfast.skyblockfeatures.utils.ItemUtils;
import mrfast.skyblockfeatures.utils.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class PricingData {

    public static final String dataURL = "https://moulberry.codes/lowestbin.json";
    public static final HashMap<String, Double> lowestBINs = new HashMap<>();
    public static final HashMap<String, Double> averageLowestBINs = new HashMap<>();
    public static final HashMap<String, Double> bazaarPrices = new HashMap<>();
    public static final HashMap<String, Integer> npcSellPrices = new HashMap<>();

    static JsonObject auctionPricesJson = null;
    public static final StopWatch reloadTimer = new StopWatch();

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static int getTier(String str) {
        int tier = 0;
        switch (str) {
            case "COMMON":tier = 0;break;
            case "UNCOMMON":tier = 1;break;
            case "RARE":tier = 2;break;
            case "EPIC":tier = 3;break;
            case "LEGENDARY":tier = 4;break;
            case "MYTHIC":tier = 5;break;
        }
        return tier;
    }

    public static String getIdentifier(ItemStack item) {
        NBTTagCompound extraAttr = ItemUtils.getExtraAttributes(item);
        String id = ItemUtils.getSkyBlockItemID(extraAttr);
        if (id == null) return null;
        switch (id) {
            case "PET":
                if (extraAttr.hasKey("petInfo")) {
                    JsonObject petInfo = gson.fromJson(extraAttr.getString("petInfo"), JsonObject.class);
                    if (petInfo.has("type") && petInfo.has("tier")) {
                        id = petInfo.get("type").getAsString() + ";" + getTier(petInfo.get("tier").getAsString());
                    }
                }
            break;
            case "ENCHANTED_BOOK":
                if (extraAttr.hasKey("enchantments")) {
                    NBTTagCompound enchants = extraAttr.getCompoundTag("enchantments");
                    if (!enchants.hasNoTags()) {
                        String enchant = enchants.getKeySet().iterator().next();
                        id = "ENCHANTMENT_"+enchant.toUpperCase(Locale.US) + "_" + enchants.getInteger(enchant);
                    }
                }
            break;
            case "POTION":
                if (extraAttr.hasKey("potion") && extraAttr.hasKey("potion_level")) {
                    id = "POTION";
                }
            break;
        }
        return id;
    }

    public static JsonObject getItemAuctionInfo(String internalname) {
		if (auctionPricesJson == null) return null;
		JsonElement e = auctionPricesJson.get(internalname);
		if (e == null) {
			return null;
		}
		return e.getAsJsonObject();
	}

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !Utils.inSkyblock) return;
        if (reloadTimer.getTime() >= 90000 || !reloadTimer.isStarted()) {
            if(reloadTimer.getTime() >= 90000) reloadTimer.reset();
            reloadTimer.start();
            // Load average lowest binss
            new Thread(() -> {
                JsonObject data = APIUtils.getJSONResponse(dataURL);
                for (Map.Entry<String, JsonElement> items : data.entrySet()) {
                    lowestBINs.put(items.getKey(), Math.floor(items.getValue().getAsDouble()));
                }
                AuctionUtil.getMyApiGZIPAsync("https://moulberry.codes/auction_averages_lbin/1day.json.gz", (jsonObject) -> {
                    for (Map.Entry<String, JsonElement> items : jsonObject.entrySet()) {
                        averageLowestBINs.put(items.getKey(), Math.floor(items.getValue().getAsDouble()));
                    }
                }, ()->{});
            }).start();
            // Get extra auction data
            if (SkyblockFeatures.config.auctionGuis || SkyblockFeatures.config.autoAuctionFlip) {
                new Thread(() -> {
                    AuctionUtil.getMyApiGZIPAsync("https://moulberry.codes/auction_averages/3day.json.gz", (jsonObject) -> {
                        auctionPricesJson = jsonObject;
                    }, ()->{});
                }).start();
            }
            // Get bazaar prices
            if (bazaarPrices.size() == 0 && SkyblockFeatures.config.apiKey.length()>1) {
                new Thread(() -> {
                    JsonObject data = APIUtils.getJSONResponse("https://api.hypixel.net/skyblock/bazaar?key="+SkyblockFeatures.config.apiKey);
                    JsonObject products = data.get("products").getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
                        if (entry.getValue().isJsonObject()) {
                            JsonObject product = entry.getValue().getAsJsonObject();
                            JsonObject quickStatus = product.get("quick_status").getAsJsonObject();
                            Double sellPrice = Math.floor(quickStatus.get("sellPrice").getAsDouble());
                            String id = quickStatus.get("productId").toString().split(":")[0];
                            bazaarPrices.put(id.replace("\"", ""), sellPrice);
                        }
                    }
                }).start();
            }
            // Get NPC sell prices
            if (npcSellPrices.size() == 0) {
                new Thread(() -> {
                    JsonObject data = APIUtils.getJSONResponse("https://api.hypixel.net/resources/skyblock/items");
                    JsonArray items = data.get("items").getAsJsonArray();
                    for (JsonElement item : items) {
                        JsonObject json = item.getAsJsonObject();
                        try {
                            npcSellPrices.put(json.get("id").getAsString(), json.get("npc_sell_price").getAsInt());
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                    }
                }).start();
            }
        }
    }
}
