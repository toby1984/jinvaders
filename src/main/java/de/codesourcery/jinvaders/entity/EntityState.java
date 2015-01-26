package de.codesourcery.jinvaders.entity;

public enum EntityState
{
	ALIVE{
		@Override
		public boolean canTransitionTo(EntityState other) {
			return other == DYING;
		}
	},
	DYING{
		@Override
		public boolean canTransitionTo(EntityState other) {
			return other == DEAD;
		}
	},
	DEAD;

	public boolean canTransitionTo(EntityState other) { return false; }
}