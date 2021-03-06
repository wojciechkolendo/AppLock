package com.wojciechkolendo.applock.models;

import org.litepal.crud.DataSupport;

/**
 * Class for protected app.
 */

public class ProtectedApplication extends DataSupport {

	private String packageName;

	public ProtectedApplication() {
		this(null);
	}

	public ProtectedApplication(String name) {
		this.packageName = name;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ProtectedApplication)) {
			return false;
		}
		return ((ProtectedApplication) obj).getPackageName().equals(this.packageName);
	}
}
