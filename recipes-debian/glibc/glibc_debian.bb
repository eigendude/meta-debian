#
# base recipe: meta/recipes-core/glibc/glibc_2.27.bb
# base branch: master
# base commit: a5d1288804e517dee113cb9302149541f825d316
#

require recipes-core/glibc/glibc.inc

inherit debian-package
require recipes-debian/sources/glibc.inc
BPN = "glibc"

LICENSE = "GPLv2 & LGPLv2.1"
LIC_FILES_CHKSUM = " \
file://COPYING;md5=b234ee4d69f5fce4486a80fdaf4a4263 \
file://COPYING.LIB;md5=4fbd65380cdd255951079008b364516c \
file://LICENSES;md5=ebc14508894997e6daaad1b8ffd53a15 \
"

DEPENDS += "gperf-native bison-native"

FILESPATH_append = ":${COREBASE}/meta/recipes-core/glibc/glibc"

# Ignore patches 0018, 0019 and 0020 because Debian already has similar patches.
SRC_URI += " \
           file://etc/ld.so.conf \
           file://generate-supported.mk \
           \
           ${NATIVESDKFIXES} \
           file://0005-fsl-e500-e5500-e6500-603e-fsqrt-implementation.patch \
           file://0006-readlib-Add-OECORE_KNOWN_INTERPRETER_NAMES-to-known-.patch \
           file://0007-ppc-sqrt-Fix-undefined-reference-to-__sqrt_finite.patch \
           file://0008-__ieee754_sqrt-f-are-now-inline-functions-and-call-o.patch \
           file://0009-Quote-from-bug-1443-which-explains-what-the-patch-do.patch \
           file://0010-eglibc-run-libm-err-tab.pl-with-specific-dirs-in-S.patch \
           file://0011-__ieee754_sqrt-f-are-now-inline-functions-and-call-o.patch \
           file://0012-sysdeps-gnu-configure.ac-handle-correctly-libc_cv_ro.patch \
           file://0013-Add-unused-attribute.patch \
           file://0014-yes-within-the-path-sets-wrong-config-variables.patch \
           file://0015-timezone-re-written-tzselect-as-posix-sh.patch \
           file://0016-Remove-bash-dependency-for-nscd-init-script.patch \
           file://0017-eglibc-Cross-building-and-testing-instructions.patch \
           file://0022-eglibc-Forward-port-cross-locale-generation-support.patch \
           file://0023-Define-DUMMY_LOCALE_T-if-not-defined.patch \
           file://0024-elf-dl-deps.c-Make-_dl_build_local_scope-breadth-fir.patch \
           file://0025-locale-fix-hard-coded-reference-to-gcc-E.patch \
           file://0026-reset-dl_load_write_lock-after-forking.patch \
           file://0027-Acquire-ld.so-lock-before-switching-to-malloc_atfork.patch \
           file://0028-bits-siginfo-consts.h-enum-definition-for-TRAP_HWBKP.patch \
           file://0029-Replace-strncpy-with-memccpy-to-fix-Wstringop-trunca.patch \
           file://0030-plural_c_no_preprocessor_lines.patch \
            "

NATIVESDKFIXES ?= ""
NATIVESDKFIXES_class-nativesdk = "\
           file://0001-nativesdk-glibc-Look-for-host-system-ld.so.cache-as-.patch \
           file://0002-nativesdk-glibc-Fix-buffer-overrun-with-a-relocated-.patch \
           file://0003-nativesdk-glibc-Raise-the-size-of-arrays-containing-.patch \
           file://0004-nativesdk-glibc-Allow-64-bit-atomics-for-x86.patch \
           file://relocate-locales.patch \
           file://0031-nativesdk-deprecate-libcrypt.patch \
"

B = "${WORKDIR}/build-${TARGET_SYS}"

PACKAGES_DYNAMIC = ""

# the -isystem in bitbake.conf screws up glibc do_stage
BUILD_CPPFLAGS = "-I${STAGING_INCDIR_NATIVE}"
TARGET_CPPFLAGS = "-I${STAGING_DIR_TARGET}${includedir}"

GLIBC_BROKEN_LOCALES = ""

GLIBCPIE ??= ""

EXTRA_OECONF = "--enable-kernel=${OLDEST_KERNEL} \
                --disable-profile \
                --disable-debug --without-gd \
                --enable-clocale=gnu \
                --with-headers=${STAGING_INCDIR} \
                --without-selinux \
                --enable-obsolete-rpc \
                --enable-tunables \
                --enable-bind-now \
                --enable-stack-protector=strong \
                --enable-stackguard-randomization \
                ${GLIBCPIE} \
                ${GLIBC_EXTRA_OECONF}"

EXTRA_OECONF += "${@get_libc_fpu_setting(bb, d)}"
EXTRA_OECONF += "${@bb.utils.contains('DISTRO_FEATURES', 'libc-inet-anl', '--enable-nscd', '--disable-nscd', d)}"

do_patch_append() {
    bb.build.exec_func('do_fix_readlib_c', d)
}

do_fix_readlib_c () {
	sed -i -e 's#OECORE_KNOWN_INTERPRETER_NAMES#${EGLIBC_KNOWN_INTERPRETER_NAMES}#' ${S}/elf/readlib.c
}

do_configure () {
	echo "config:" >> ${S}/Makeconfig
	sed -i -e "s:manual::" ${S}/Makeconfig

	# override this function to avoid the autoconf/automake/aclocal/autoheader
	# calls for now
	# don't pass CPPFLAGS into configure, since it upsets the kernel-headers
	# version check and doesn't really help with anything
	(cd ${S} && gnu-configize) || die "failure in running gnu-configize"
	find ${S} -name "configure" | xargs touch
	CPPFLAGS="" oe_runconf
}

do_compile () {
	# -Wl,-rpath-link <staging>/lib in LDFLAGS can cause breakage if another glibc is in staging
	unset LDFLAGS
	base_do_compile
	echo "Adjust ldd script"
	if [ -n "${RTLDLIST}" ]
	then
		prevrtld=`cat ${B}/elf/ldd | grep "^RTLDLIST=" | sed 's#^RTLDLIST="\?\([^"]*\)"\?$#\1#'`
		if [ "${prevrtld}" != "${RTLDLIST}" ]
		then
			sed -i ${B}/elf/ldd -e "s#^RTLDLIST=.*\$#RTLDLIST=\"${prevrtld} ${RTLDLIST}\"#"
		fi
	fi
}

require recipes-core/glibc/glibc-package.inc

do_poststash_install_cleanup_append() {
	# We don't want to ship an empty /usr/share
	if [ -d ${D}${datadir} ]; then
		rmdir --ignore-fail-on-non-empty ${D}${datadir}
	fi
}

BBCLASSEXTEND = "nativesdk"
