class DNSLookup {
	
	public String lookup ( String fqdn ) throws Exception {
		return java.net.InetAddress.getByName(fqdn).getHostAddress();
	}

	public String reverseLookup ( String ip ) throws Exception {
		String fqdn = java.net.InetAddress.getByName(ip).getCanonicalHostName();
		if(fqdn.equals(ip)) throw new Exception("Unable to resolve hostname");
		return fqdn;
	}
}