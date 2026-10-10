#define _GNU_SOURCE
#include <ftw.h>
#include <string.h>
#include <stdio.h>

static void (*g_cb)(const char*) = NULL;
static int g_count = 0;

static int visit(const char* path, const struct stat* st, int flag, struct FTW* ftw) {
    if (flag == FTW_F && g_cb) {
        g_cb(path);
        g_count++;
    }
    return 0;
}

int lab_walk_files(const char* root, void (*cb)(const char*)) {
    g_cb = cb;
    g_count = 0;
    nftw(root, visit, 16, FTW_PHYS);
    return g_count;
}
