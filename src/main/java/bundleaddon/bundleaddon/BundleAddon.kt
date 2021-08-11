package bundleaddon.bundleaddon

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BundleMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class BundleAddon : JavaPlugin(),Listener {

    private val prefix = "§f[§e§lBundle§a§lAddon§f]§r"

    override fun onEnable() {
        getCommand("bundleaddon")?.setExecutor(this)
        server.pluginManager.registerEvents(this,this)
    }

    private fun Player.sendmsg(s : String){
        this.sendMessage(prefix + s)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player)return true
        if (!sender.hasPermission("bundle.user")){
            sender.sendmsg("§4You don't have permission!")
            return true
        }
        if (args.isEmpty()){
            val mainhand = sender.inventory.itemInMainHand
            val offhand = sender.inventory.itemInOffHand

            if (mainhand.type != Material.BUNDLE){
                sender.sendmsg("§4右手にバンドルを持ってください")
                return true
            }

            if (offhand.type == Material.AIR){
                sender.sendmsg("§4左手にバンドルに入れるアイテムを持ってください")
                return true
            }

            if (offhand.type == Material.BUNDLE){
                sender.sendmsg("§4左手にバンドルを持たないでください")
                return true
            }

            val bundlemeta = mainhand.itemMeta as BundleMeta
            val limit = bundlemeta.persistentDataContainer[NamespacedKey.fromString("bundlelimit")!!, PersistentDataType.INTEGER]
            if (limit == null){
                sender.sendmsg("§4このアイテムはBundleAddonのアイテムではありません")
                return true
            }
            var nowbundlesize = 0
            if (bundlemeta.hasItems()){
                for (size in bundlemeta.items){
                    nowbundlesize += size.amount
                }
            }

            val inputamount = limit-nowbundlesize
            if (inputamount < 1){
                sender.sendmsg("§4バンドルの上限を超えています")
                return true
            }
            if (inputamount > offhand.amount){
                val clone = offhand.clone()
                clone.amount = offhand.amount
                sender.inventory.setItemInOffHand(ItemStack(Material.AIR))
                bundlemeta.addItem(clone)
            }else{
                val clone = offhand.clone()
                clone.amount = inputamount
                offhand.amount -= inputamount
                bundlemeta.addItem(clone)
            }

            mainhand.itemMeta = bundlemeta
            sender.sendmsg("§aアイテムを入れました")
            return true
        }

        if (args[0] == "limit" && args.size == 2){
            if (!sender.hasPermission("bundle.op")){
                sender.sendmsg("§4You don't have permission!")
                return true
            }
            val mainhand = sender.inventory.itemInMainHand

            if (mainhand.type != Material.BUNDLE){
                sender.sendmsg("§4右手にバンドルを持ってください")
                return true
            }

            val value = args[1].toIntOrNull()
            if (value == null){
                sender.sendmsg("§4limitは数字で指定してください")
                return true
            }
            val bundlemeta = mainhand.itemMeta as BundleMeta
            bundlemeta.persistentDataContainer.set(NamespacedKey.fromString("bundlelimit")!!, PersistentDataType.INTEGER,value)
            mainhand.itemMeta = bundlemeta

            sender.sendmsg("§blimitを${value}にしました")
            return true
        }


        return true
    }

    @EventHandler
    fun bundleclick(e : PlayerInteractEvent){
        if (e.hand == EquipmentSlot.OFF_HAND)return
        if (e.action != Action.RIGHT_CLICK_AIR)return
        if (!e.hasItem())return
        if (!e.player.hasPermission("bundle.user"))return
        if (e.item!!.type != Material.BUNDLE)return
        if (e.player.location.pitch < -80f){
            e.isCancelled = true
            val mainhand = e.item!!
            val bundlemeta = mainhand.itemMeta as BundleMeta
            val limit = bundlemeta.persistentDataContainer[NamespacedKey.fromString("bundlelimit")!!, PersistentDataType.INTEGER]
                ?: return
            var nowbundlesize = 0
            if (bundlemeta.hasItems()){
                for (size in bundlemeta.items){
                    nowbundlesize += size.amount
                }
            }

            var inputamount = limit-nowbundlesize
            if (inputamount < 1){
                e.player.sendmsg("§4バンドルの上限を超えています")
                return
            }

            for (content in e.player.inventory.contents){
                if (content == null)continue
                if (content.type == Material.BUNDLE)continue
                if (inputamount < content.amount){
                    val clone = content.clone()
                    val amount = content.amount - inputamount
                    clone.amount -= amount
                    bundlemeta.addItem(clone)
                    content.amount -= amount
                    inputamount -= amount
                    break
                }else{
                    val clone = content.clone()
                    bundlemeta.addItem(clone)
                    inputamount -= clone.amount
                    content.amount = 0
                }
            }

            mainhand.itemMeta = bundlemeta
            e.player.sendmsg("§aアイテムを入れました")

        }

    }
}