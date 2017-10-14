package com.SHGroup.MissileSystem;

import java.util.ArrayList;

import org.bukkit.entity.FallingBlock;

public class LaunchedMissileData {
	public Missile m;
	public ArrayList<FallingBlock> fbs = new ArrayList<FallingBlock>();
	public byte temp = 0;
	public int toX = 0;
	public int toZ = 0;
	public String launchedPerson;
	public LaunchedMissileData(String launchedPerson, Missile m, ArrayList<FallingBlock> fbs,
			int toX, int toZ){
		this.launchedPerson = launchedPerson;
		this.m = m;
		this.fbs = fbs;
		this.toX = toX;
		this.toZ = toZ;
	}
}
