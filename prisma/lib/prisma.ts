import { PrismaClient } from "prismaclient";

let prisma: PrismaClient | null = null;

export async function getPrisma(): Promise<PrismaClient> {
    if (!prisma) {
        prisma = new PrismaClient();
        await prisma.$connect();
        await prisma.$queryRawUnsafe("PRAGMA journal_mode=WAL");
        await prisma.$queryRawUnsafe("PRAGMA busy_timeout=5000");
    }
    return prisma;
}
