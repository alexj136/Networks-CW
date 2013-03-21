//Candidate No: 18512

/**
 * Looks and reverse looks up the domain name system for IP addresses/FQDNs
 */
class DNSLookup {
	
	/**
	 * Looks up an IP address from an FQDN
	 * @param fqdn The fqdn to look up
	 * @return The corresponding IP address
	 */
	public String lookup ( String fqdn ) throws Exception {
		return java.net.InetAddress.getByName(fqdn).getHostAddress();
	}
	
	/**
	 * Looks up an FQDN from an IP address
	 * @param ip The ip to look up
	 * @return A corresponding FQDN
	 */
	public String reverseLookup ( String ip ) throws Exception {
		String fqdn = java.net.InetAddress.getByName(ip).getCanonicalHostName();
		if(fqdn.equals(ip)) throw new Exception("Unable to resolve hostname");
		return fqdn;
	}
}