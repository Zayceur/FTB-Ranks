package dev.ftb.mods.ftbranks.impl;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbranks.api.PermissionValue;
import dev.ftb.mods.ftbranks.api.Rank;
import dev.ftb.mods.ftbranks.api.RankCondition;
import dev.ftb.mods.ftbranks.api.RankManager;
import dev.ftb.mods.ftbranks.api.event.*;
import dev.ftb.mods.ftbranks.impl.condition.DefaultCondition;

import java.time.Instant;
import java.util.*;

/**
 * @author LatvianModder
 */
public class RankImpl implements Rank, Comparable<RankImpl> {
	public final RankManagerImpl manager;
	public final String id;
	public final Map<String, PermissionValue> permissions;
	public String name;
	public int power;
	public RankCondition condition;

	public RankImpl(RankManagerImpl m, String s) {
		manager = m;
		id = s;
		permissions = new LinkedHashMap<>();
		name = "";
		power = 50;
		condition = new DefaultCondition(this);
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		return o == this || o instanceof Rank && id.equals(((Rank) o).getId());
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public RankManager getManager() {
		return manager;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getPower() {
		return power;
	}

	@Override
	public void setPermission(String node, PermissionValue value) {
		if (node.equals("condition")) {
			throw new IllegalArgumentException("use '/ftbranks condition' to set conditions");
		}

		PermissionValue oldValue = getPermission(node);
		if (!oldValue.equals(value)) {
			if (value != null) {
				permissions.put(node, value);
			} else {
				permissions.remove(node);
			}
			RankEvent.PERMISSION_CHANGED.invoker().accept(new PermissionNodeChangedEvent(manager, this, node, oldValue, value));
			manager.saveRanks();
		}
	}

	@Override
	public PermissionValue getPermission(String node) {
		return permissions.getOrDefault(node, PermissionValue.DEFAULT);
	}

	@Override
	public RankCondition getCondition() {
		return condition;
	}

	@Override
	public void setCondition(RankCondition condition) {
		RankCondition oldCondition = this.condition;
		this.condition = condition;
		RankEvent.CONDITION_CHANGED.invoker().accept(new ConditionChangedEvent(manager, this, oldCondition, condition));
		manager.saveRanks();
	}

	@Override
	public boolean add(GameProfile profile) {
		PlayerRankData data = manager.getPlayerData(profile);

		if (!data.added.containsKey(this)) {
			data.added.put(this, Instant.now());
			manager.savePlayers();
			RankEvent.ADD_PLAYER.invoker().accept(new PlayerAddedToRankEvent(manager, this, profile));
			return true;
		}

		return false;
	}

	@Override
	public boolean remove(GameProfile profile) {
		PlayerRankData data = manager.getPlayerData(profile);

		if (data.added.remove(this) != null) {
			RankEvent.REMOVE_PLAYER.invoker().accept(new PlayerRemovedFromRankEvent(manager,this, profile));
			manager.savePlayers();
			return true;
		}

		return false;
	}

	@Override
	public int compareTo(RankImpl o) {
		return o.getPower() - getPower();
	}

	@Override
	public Collection<String> getPermissions() {
		Set<String> nodes = new HashSet<>(permissions.keySet());
		nodes.remove("name");
		nodes.remove("power");
		return nodes;
	}
}