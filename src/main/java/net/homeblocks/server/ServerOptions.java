package net.homeblocks.server;

public record ServerOptions(int clearPort, int tlsPort, String tlsCertPath, String tlsKeyPath) {}
