package com.SHGroup.MissileSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class Main extends JavaPlugin implements Listener{
	private ArrayList<Missile> missiles = new ArrayList<Missile>();
	private ArrayList<Runnable> run = new ArrayList<Runnable>();
	private HashMap<String, Block> input = new HashMap<String, Block>();
	private final ArrayList<LaunchedMissileData> entityList = new ArrayList<LaunchedMissileData>();
	private Config config;
	private long temp = 0l;
	@Override
	public void onEnable(){
		config = new Config();
		try{
			File f = new File("plugins/MissileSystem/config.yml");
			if(!f.exists()){
				f.getParentFile().mkdirs();
				f.createNewFile();
				setDefaultConfig();
				config.save(f);
			}
			config.load(f);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		loadConfig();
		this.getServer().getPluginManager().registerEvents(this, this);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				runAtTick();
				temp += 1;
				if(temp == 20){
					temp = 0;
					new Thread(){@Override public void run(){runAtSec();}}.start();
				}
				if(!run.isEmpty()){
					Object[] runs = run.toArray();
					for(Object r : runs){
						((Runnable)r).run();
						run.remove(r);
					}
				}
			}
		}, 1l, 1l);
	}
	@Override
	public void onDisable(){
		Bukkit.getScheduler().cancelTasks(this);
	}
	
	private void runAtTick(){
		ArrayList<LaunchedMissileData> list = new ArrayList<LaunchedMissileData>();
		list.addAll(entityList);
		for(LaunchedMissileData data : list){
			Location loc = data.fbs.get(0).getLocation().clone();
			ParticleEffect.SMOKE_LARGE.display(0.0f, 0.0f, 0.0f, 0, 5, loc, Bukkit.getOnlinePlayers());
			if(data.fbs.get(0).isDead()){
				entityList.remove(data);
				for(FallingBlock b : data.fbs){
					b.remove();
				}
				data.fbs.clear();
				startExpode(data.m, loc);
				continue;
			}
			if(!loc.getChunk().isLoaded()){
				loc.getChunk().load();
			}
			if(data.temp == 0){
				loc = data.fbs.get(data.fbs.size() - 1).getLocation().clone();
				if (loc.getY() > 200) {
					data.temp = 1;
					return;
				}
				if (!loc.add(0, 20, 0).getChunk().isLoaded()) {
					loc.add(0, 20, 0).getChunk().load();
				}
				ArrayList<FallingBlock> fbs = new ArrayList<FallingBlock>();
				fbs.addAll(data.fbs);
				for (FallingBlock fb : fbs) {
					fb.setVelocity(new Vector(0, 1, 0));
				}
			}else if(data.temp == 1){
				Location toLoc = new Location(loc.getWorld(), data.toX,
						loc.getY(), data.toZ);
				loc = data.fbs.get(data.fbs.size() - 1).getLocation()
						.clone();
				if (loc.distance(toLoc) < 5) {
					data.temp = 2;
					return;
				}
				if (!loc.getChunk().isLoaded()) {
					loc.getChunk().load();
				}
				ArrayList<FallingBlock> fbs = new ArrayList<FallingBlock>();
				fbs.addAll(data.fbs);
				for (FallingBlock fb : fbs) {
					fb.setVelocity(getToVector(loc, toLoc).setY(0)
							.multiply(0.1));
				}
			}else if(data.temp > 1){
				if(data.temp == 2){
					World w = loc.getWorld();
					Location spawnLoc = loc.clone();
					int i = 0;
					if (!spawnLoc.getChunk().isLoaded()) {
						spawnLoc.getChunk().load();
					}
					ArrayList<FallingBlock> fbs = new ArrayList<FallingBlock>();
					fbs.addAll(data.fbs);
					data.fbs.clear();
					for (FallingBlock fb : fbs) {
						BlockId id = data.m.ids.get(i);
						FallingBlock fbn = w.spawnFallingBlock(
								spawnLoc.add(0, i, 0), id.id,
								(byte) (id.dur == -1 ? 0 : id.dur));
						fbn.setVelocity(new Vector(0.0, -1.5, 0.0));
						data.fbs.add(fbn);
						fb.remove();
						i += 1;
					}
					data.temp = 3;
				}
			}
		}
	}
	private void runAtSec(){
		for(Map.Entry<String, Block> datas : input.entrySet()){
			if(Bukkit.getPlayerExact(datas.getKey()) == null){
				input.remove(datas.getKey());
			}
			if(getMissile(datas.getValue()) == null){
				input.remove(datas.getKey());
			}
		}
		ArrayList<Player> list = new ArrayList<Player>();
		for(LaunchedMissileData data : entityList){
			Player p = Bukkit.getPlayerExact(data.launchedPerson);
			if(p != null){
				if(list.contains(p)){
					continue;
				}
				list.add(p);
				p.sendMessage("§c§l" + data.m.name + "§e§l은(는) 현재 " + data.fbs.get(0).getLocation().getBlockX()
						+ "," + data.fbs.get(0).getLocation().getBlockY() 
						+ "," + data.fbs.get(0).getLocation().getBlockZ() + "에 있습니다.");
			}
		}
	}
	private Vector getToVector(Location from, Location to) {
		return to.toVector().subtract(from.toVector());
	}
	
	public void runAtBukkitTask(Runnable r){
		this.run.add(r);
	}
	
	public void startExpode(final Missile m, final Location l){
		new Thread(){
			@Override
			public void run(){
				Bukkit.broadcastMessage("§4[ 경고 ] §e§l" + l.getBlockX() + " / " + l.getBlockY()
						+ " / " + l.getBlockZ() + " 에 §c§l" + m.name + "§e§l이(가) 터졌습니다!");
				ParticleEffect.EXPLOSION_HUGE.display(0.0f, 0.0f, 0.0f, 1, 3, l, Bukkit.getOnlinePlayers());
				int bX = l.getBlockX();
				int bY = l.getBlockY();
				int bZ = l.getBlockZ();
				
				int radius = m.spreadrange;
		
				if(m.damage > 0){
					runAtBukkitTask(new Runnable() {
						@Override
						public void run() {
							for(Entity e : l.getWorld().getEntities()){
								if(e.getLocation().distance(l) < m.spreadrange){
									if(e instanceof LivingEntity){
										LivingEntity entity = (LivingEntity)e;
										entity.damage(m.damage);
									}
								}
							}
						}
					});
				}
				ArrayList<Integer> list = new ArrayList<Integer>();
				if(!m.antiBlocks.isEmpty()){
					for(int i = 0 ; i < m.antiBlocks.size() ; i ++){
						BlockId id = m.antiBlocks.get(i);
						list.add(id.id);
					}
				}
				boolean isEmpty = list.isEmpty();
				ArrayList<Location> locs = new ArrayList<Location>();
				for(int x = bX - radius; x <= bX + radius; x++){
					for(int y = bY - radius; y <= bY + radius ; y++){
						for(int z = bZ - radius; z <= bZ + radius ; z++){
							double distance = ((bX - x) * (bX - x)) + ((bZ - z) * (bZ - z)) + ((bY - y) * (bY - y));
							if(distance < radius * radius 
									&& !(false)){
								Location loc = new Location(l.getWorld(), x, y, z);
								Block b = loc.getBlock();
								if(!isEmpty){
									if(list.contains(b.getTypeId())){
										boolean continu = false;
										for(BlockId id : m.antiBlocks){
											if(id.equals(b)){
												continu = true;
												break;
											}
										}
										if(continu){
											continue;
										}
									}
								}
								locs.add(loc);
							}
						}
					}
				}
				final ArrayList<Location> loc = new ArrayList<Location>();
				loc.addAll(locs);
				runAtBukkitTask(new Runnable() {
					@Override
					public void run() {
						int replaceid = m.replace.id;
						byte replacedata = (byte)m.replace.dur;
						if(replaceid < 0){
							replaceid = 0;
						}
						if(replacedata < 0){
							replacedata = (byte)0;
						}
						for(Location l : loc){
							l.getBlock().setTypeId(replaceid);
							l.getBlock().setData(replacedata);
						}
						l.getWorld().playSound(l, Sound.EXPLODE, 10.0f, 10.0f);
					}
				});
			}
		}.start();
	}
	
	public void launchMissile(final String launchedPerson, final Missile m, final Block center, final int toX, final int toZ){
		runAtBukkitTask(new Runnable() {
			@Override
			public void run() {
				final ArrayList<Block> blocks = new ArrayList<Block>();
				for(int i = 0 ; i < m.ids.size() ; i ++){
					Block tempblock = center.getLocation().add(0,i,0).getBlock();
					if(m.ids.get(0).equals(tempblock)){
						for(int i2 = 0 ; i2 < m.ids.size() ; i2 ++){
							Block nowBlock = tempblock.getLocation().add(0, -i2, 0).getBlock();
							if(m.ids.get(i2).equals(nowBlock)){
								blocks.add(nowBlock);
							}
						}
						if(blocks.size() != m.ids.size()){
							blocks.clear();
						}
					}
				}
				if(blocks.isEmpty()){
					return;
				}
				final ArrayList<Location> locs = new ArrayList<Location>();
				for(Block b : blocks){
					locs.add(b.getLocation().clone());
					b.setType(Material.AIR);
				}
				blocks.clear();
				for(int i = 0 ; i < m.shootamount ; i ++ ){
					Bukkit.getScheduler().scheduleSyncDelayedTask(Main.this, new Runnable() {
						public void run() {
							ArrayList<FallingBlock> fbs = new ArrayList<FallingBlock>();
							int i = 0;
							for(BlockId id : m.ids){
								fbs.add(locs.get(i).getWorld().spawnFallingBlock(locs.get(i).add(0, 1.5, 0), id.id, (byte) (id.dur==-1?0:id.dur)));
								i += 1;
							}
							entityList.add(new LaunchedMissileData(launchedPerson, m, fbs, toX, toZ));
							locs.get(0).getWorld().playSound(locs.get(0), Sound.GHAST_FIREBALL, 10.0f, 10.0f);
						}
					}, i * 10);
				}
		}
	});
	}
	@EventHandler
	public void intreact(PlayerInteractEvent e){
		if(e.getClickedBlock() != null){
			Missile m = getMissile(e.getClickedBlock());
			if(m != null){
				input.put(e.getPlayer().getName(), e.getClickedBlock());
				e.getPlayer().sendMessage("§a미사일을 날릴 좌표를 입력해주세요. (x좌표/z좌표 형식 - 예: 32/-283)");
				e.getPlayer().sendMessage("§a" + m.name + "의 최대 범위는 " + Integer.toString(m.range) + "입니다.");
				e.getPlayer().sendMessage("§a현재 좌표는 X : " + Integer.toString((int)e.getPlayer().getLocation().getX()) + ", Z : "
						+ Integer.toString((int)e.getPlayer().getLocation().getZ()) + "입니다.");
				e.getPlayer().sendMessage("§a(취소하시려면 \'취소\'라고 입력해주세요.)");
			}
		}
	}
	@EventHandler
	public void chat(AsyncPlayerChatEvent e){
		Player p = e.getPlayer();
		if(input.containsKey(p.getName())){
			String loc = e.getMessage();
			Block target = input.get(p.getName());
			Missile m = getMissile(target);
			if(loc.equals("취소")){
				p.sendMessage("§c입력이 취소되었습니다.");
				input.remove(p.getName());
				return;
			}
			if(loc.split("/").length != 2){
				p.sendMessage("§c좌표 형식이 올바르지 않습니다. 입력이 취소되었습니다.");
				input.remove(p.getName());
				return;
			}
			try{
				int x = Integer.parseInt(loc.split("/")[0]);
				int z = Integer.parseInt(loc.split("/")[1]);
				Location l = target.getLocation().clone();
				Location targetl = target.getLocation().clone();
				if(l.distanceSquared(targetl) > m.range){
					p.sendMessage("§c거리가 너무 멉니다. 입력이 취소되었습니다.");
					input.remove(p.getName());
					return;
				}
				launchMissile(p.getName(), m, target, x, z);
				p.sendMessage("§e" + m.name + "을(를) 발사하였습니다!");
			}catch(Exception ex){
				ex.printStackTrace();
				p.sendMessage("§c좌표 형식이 올바르지 않습니다. 입력이 취소되었습니다.");
			}finally{
				input.remove(p.getName());
			}
		}
	}
	


	@EventHandler
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		if (event.getEntity() instanceof FallingBlock) {
			Object[] datas = entityList.toArray();
			for (Object d : datas) {
				LaunchedMissileData data = (LaunchedMissileData)d;
				if(data.fbs.contains(event.getEntity())){
					entityList.remove(data);
					event.getBlock().setType(Material.AIR);
					event.getEntity().remove();
					for(FallingBlock fb : data.fbs){
						fb.remove();
					}
					data.fbs.clear();
					startExpode(data.m, event.getBlock().getLocation());
				}
			}
		}
	}
	
	
	public Missile getMissile(Block b){
		for(Missile m : missiles){
			for(int i = 0 ; i < m.ids.size() ; i ++){
				Block tempblock = b.getLocation().add(0,i,0).getBlock();
				if(m.ids.get(0).equals(tempblock)){
					boolean not = false;
					for(int i2 = 0 ; i2 < m.ids.size() ; i2 ++){
						if(!m.ids.get(i2).equals(tempblock.getLocation().add(0, -i2, 0).getBlock())){
							not = true;
							break;
						}
					}
					if(!not){
						return m;
					}
				}
			}
		}
		return null;
	}

	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if(missiles.isEmpty()){
			sender.sendMessage("§c미사일 목록이 비었습니다.");
			return true;
		}
		sender.sendMessage("§a-=-=- 미사일 목록 -=-=-");
		for(Missile m : missiles){
			sender.sendMessage("§e이름 : " + m.name + "/ 폭발 범위 : " + Integer.toString(m.spreadrange));
		}
		sender.sendMessage("§7[플러그인 제작 : SHGroup]");
		return true;
	}
	
	
	
	
	private void setDefaultConfig() {
		configSet(new Missile("미사일", 200, 20, 1, 18, new BlockId(0,-1), 
				Arrays.asList(new BlockId(44,-1), new BlockId(35,0), new BlockId(42, -1), new BlockId(46,-1)),
				Arrays.asList(new BlockId(7, -1), new BlockId(49, -1))));
		configSet(new Missile("장거리미사일", 500, 15, 1, 15, new BlockId(0,-1), 
				Arrays.asList(new BlockId(44,-1), new BlockId(35,15), new BlockId(42, -1), new BlockId(46,-1)),
				Arrays.asList(new BlockId(7, -1), new BlockId(49, -1))));
		configSet(new Missile("용암미사일", 150, 8, 1, 0, new BlockId(10,-1), 
				Arrays.asList(new BlockId(44,-1), new BlockId(35,1), new BlockId(42, -1), new BlockId(46,-1)),
				Arrays.asList(new BlockId(7, -1), new BlockId(49, -1))));
		configSet(new Missile("핵미사일", 300, 50, 1, 30, new BlockId(0,-1), 
				Arrays.asList(new BlockId(44,-1), new BlockId(35,5), new BlockId(42, -1), new BlockId(46,-1)),
				Arrays.asList(new BlockId(7, -1))));
		configSet(new Missile("수소미사일", 300, 100, 1, 50, new BlockId(0,-1), 
				Arrays.asList(new BlockId(44,-1), new BlockId(35,9), new BlockId(42, -1), new BlockId(46,-1)),
				Arrays.asList(new BlockId(7, -1))));
		configSet(new Missile("미사일연속발사시스템", 300, 10, 8, 18, new BlockId(0,-1), 
				Arrays.asList(new BlockId(44,-1), new BlockId(35,4), new BlockId(42, -1), new BlockId(46,-1)),
				Arrays.asList(new BlockId(7, -1), new BlockId(49, -1))));
	}
	private void configSet(Missile m){
		if(!config.contains("미사일목록")){
			config.set("미사일목록", (List<String>)new ArrayList<String>());
		}
		String n = m.name;
		List<String> missiles = config.getStringList("미사일목록");
		if(!missiles.contains(n)){
			missiles.add(n);
			config.set("미사일목록", missiles);
		}
		config.set(n + ".날라가는 범위", m.range);
		config.set(n + ".폭발 범위", m.spreadrange);
		config.set(n + ".발사 개수", m.shootamount);
		config.set(n + ".데미지", m.damage);
		config.set(n + ".폭발시 범위 내의 블럭이 변경될 id", m.replace.toString());
		List<String> temp = new ArrayList<String>();
		for(BlockId id : m.ids){
			if(id == null || id.toString().equals("")){
				continue;
			}
			temp.add(id.toString());
		}
		config.set(n + ".미사일 본체", temp);
		List<String> temp2 = new ArrayList<String>();
		for(BlockId id : m.antiBlocks){
			if(id == null || id.toString().equals("")){
				continue;
			}
			temp2.add(id.toString());
		}
		config.set(n + ".부수지 못하는 블럭 id", temp2);
	}
	private void loadConfig(){
		if(!config.contains("미사일목록")){
			return;
		}
		List<String> list = config.getStringList("미사일목록");
		ArrayList<ArrayList<BlockId>> idss = new ArrayList<ArrayList<BlockId>>();
		for(String n : list){
			int range = 100;
			if(config.contains(n + ".날라가는 범위")){
				range = config.getInt(n + ".날라가는 범위");
			}
			int shootamount = 1;
			if(config.contains(n + ".발사 개수")){
				shootamount = config.getInt(n + ".발사 개수");
			}
			int spreadrange = 5;
			if(config.contains(n + ".폭발 범위")){
				spreadrange = config.getInt(n + ".폭발 범위");
			}
			int damage = 0;
			if(config.contains(n + ".데미지")){
				damage = config.getInt(n + ".데미지");
			}
			BlockId replace = new BlockId(0, -1);
			if(config.contains(n + ".폭발시 범위 내의 블럭이 변경될 id")){
				replace = BlockId.parseBlockId(config.getString(n + ".폭발시 범위 내의 블럭이 변경될 id"));
			}
			ArrayList<BlockId> ids = new ArrayList<BlockId>();
			if(config.contains(n + ".미사일 본체")){
				List<String> nlist = config.getStringList(n + ".미사일 본체");
				for(String n2 : nlist){
					BlockId id = BlockId.parseBlockId(n2);
					if(id != null){
						ids.add(id);
					}
				}
			}
			if(idss.contains(ids)){
				continue;
			}
			idss.add(ids);
			ArrayList<BlockId> antiBlocks = new ArrayList<BlockId>();
			if(config.contains(n + ".부수지 못하는 블럭 id")){
				List<String> nlist = config.getStringList(n + ".부수지 못하는 블럭 id");
				for(String n2 : nlist){
					BlockId id = BlockId.parseBlockId(n2);
					if(id != null){
						antiBlocks.add(id);
					}
				}
			}
			missiles.add(new Missile(n, range, spreadrange, shootamount, damage, replace, ids, antiBlocks));
		}
	}
}
