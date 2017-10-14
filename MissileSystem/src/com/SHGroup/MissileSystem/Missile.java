package com.SHGroup.MissileSystem;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;

public class Missile {
	String name;
	int range = 100;
	int spreadrange = 5;
	int damage = 0;
	int shootamount = 0;
	BlockId replace = new BlockId(0,-1);
	List<BlockId> ids = new ArrayList<BlockId>();
	List<BlockId> antiBlocks = new ArrayList<BlockId>();
	public Missile(String name, int range, int spreadrange,int shootamount, int damage, BlockId replace, List<BlockId> ids, List<BlockId> antiblocks){
		this.name = name;
		this.range = range;
		this.spreadrange = spreadrange;
		this.shootamount = shootamount;
		this.damage = damage;
		this.replace = replace;
		this.ids = ids;
		this.antiBlocks = antiblocks;
	}
	public boolean contains(Block b){
		try{
			for(BlockId id : ids){
				if(id.id == b.getTypeId()){
					if(id.dur != -1){
						if(id.dur == b.getData()){
							return true;
						}
					}else{
						return true;
					}
				}
			}
			return false;
		}catch(Exception ex){
			return false;
		}
	}
}
class BlockId{
	int id = 0;
	short dur = (short)0;
	public BlockId(int id, int dur){
		this.id = id;
		if(dur > Short.MAX_VALUE){
			dur = 0;
		}
		this.dur = (short)dur;
	}
	public boolean equals(BlockId id){
		if(id.id == this.id){
			if(id.dur == this.dur){
				return true;
			}
		}
		return false;
	}
	public boolean equals(Block b){
		if(id == b.getTypeId()){
			if(dur != -1){
				if(dur == b.getData()){
					return true;
				}
			}else{
				return true;
			}
		}
		return false;
	}
	public static BlockId parseBlockId(String b){
		try{
			if(b.split(":").length == 1){
				return new BlockId(Integer.parseInt(b), -1);
			}else{
				return new BlockId(Integer.parseInt(b.split(":")[0]), Integer.parseInt(b.split(":")[1]));
			}
		}catch(Exception ex){
			return null;
		}
	}
	public String toString(){
		if(dur != -1){
			return Integer.toString(id) + ":" + Short.toString(dur);
		}
		return Integer.toString(id);
	}
}
