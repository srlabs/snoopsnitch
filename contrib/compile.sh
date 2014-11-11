#! /bin/bash -e

usage()
{
	echo >&2 "Usage: $0 -t {android|host} [-f] [-h]"
    echo >&2 "   -t <target>   Target to build for"
    echo >&2 "   -f            Fast mode - only build parser"
    echo >&2 "   -g            init git submodules"
    echo >&2 "   -h            This help screen"
    exit 1
}

fast=""

while getopts hfgt: o
do
    case "$o" in
        t)      target="${OPTARG}";;
        f)      fast=1;;
        h)      usage;;
	g)      do_git=1;;
        [?])    usage;;
    esac
done
shift $(($OPTIND-1))

case ${target} in
        android|host) ;;
        *)             usage;
esac;

# set platform
MACH=$(uname -m)
KERN=$(uname -s)

case ${KERN} in
        Darwin) HOST="darwin-${MACH}";;
        Linux)  HOST="linux-${MACH}";;
        *)      echo "Unknown platform ${KERN}-${MACH}!"; exit 1;;
esac

# Link to latest successful build
LATEST=build-${HOST}-${target}-latest

if [ -z "${fast}" ];
then
	BUILD_DIR=$(mktemp -d build-XXXXXXXXXX)
else
	BUILD_DIR=${LATEST}
fi

export BASE_DIR="$( cd "$( dirname $0 )" && pwd )"
if [ -n "${do_git}" ];then
# update submodules if necessary
    if [ ! "$(ls -A libasn1c)" -a "x${fast}" = "x" ];
    then
  	(cd .. && git submodule init contrib/libasn1c)
    fi

    if [ ! "$(ls -A libosmocore)" -a "x${fast}" = "x" ];
    then
	(cd .. && git submodule init contrib/libosmocore)
    fi

    if [ ! "$(ls -A metagsm)" -a "x${fast}" = "x" ];
    then
	(cd .. && git submodule init contrib/metagsm)
    fi
fi

if [ -n "${do_git}" ];then
    if [ "x${fast}" = "x" ];
    then
	(cd .. && \
	    git submodule update contrib/libasn1c && \
	    git submodule update contrib/libosmocore && \
	    git submodule update contrib/metagsm)
    fi
fi

echo "Building on ${HOST} for ${target}..."

cd ${BUILD_DIR}
OUTPUT_DIR=`pwd`

export MSD_DESTDIR="${OUTPUT_DIR}/out"

case ${target} in
	android)
		export SYSROOT="${NDK_DIR}/platforms/android-19/arch-arm"
		export MSD_CONFIGURE_OPTS="--host arm-linux-androideabi --prefix=${MSD_DESTDIR}"
		export PATH=${PATH}:${NDK_DIR}/toolchains/arm-linux-androideabi-4.8/prebuilt/${HOST}/bin/
		export CROSS_COMPILE=arm-linux-androideabi
		export RANLIB=arm-linux-androideabi-ranlib
		export CFLAGS="--sysroot=${SYSROOT} -nostdlib"
		export CPPFLAGS="-I${NDK_DIR}/platforms/android-19/arch-arm/usr/include/"
		export LDFLAGS="--sysroot=${SYSROOT} -Wl,-rpath-link=${NDK_DIR}/platforms/android-19/arch-arm/usr/lib/,-L${NDK_DIR}/platforms/android-19/arch-arm/usr/lib/"
		export LIBS="-lc -lm"
		export METAGSM_MAKE_ARGS="-f Makefile.Android PREFIX=${MSD_DESTDIR}   DESTDIR=${MSD_DESTDIR}/metagsm   SYSROOT=${SYSROOT} install"
		;;
	host)
		export MSD_CONFIGURE_OPTS="--prefix=${MSD_DESTDIR}"
		export EXTRA_CFLAGS="-I${MSD_DESTDIR}/include -I${MSD_DESTDIR}/include/asn1c"
		export EXTRA_LDFLAGS="-L${MSD_DESTDIR}/lib"
		export METAGSM_MAKE_ARGS=""
		;;
	*)
		# Shouldn't happen
		echo "Invalid target \"${target}\""
		exit 1;
esac

mkdir -p ${MSD_DESTDIR}

# Do not build dependencies in fast mode
if [ -z "${fast}" ];
then
	TARGETS="libosmocore libasn1c libosmo-asn1-rrc"
fi

# Build OpenSSL only for Android
if [ "x${target}" = "xandroid" -a "x${fast}" = "x" ];
then
	TARGETS="${TARGETS} openssl"
fi

TARGETS="${TARGETS} metagsm"

for i in ${TARGETS}; do
    echo -n "Building $i..."
    cd $OUTPUT_DIR
    if ${BASE_DIR}/scripts/compile_$i.sh > $OUTPUT_DIR/$i.compile_log 2>&1;then
	echo OK
    else
	echo "Failed!"
	echo "Please view log file $OUTPUT_DIR/$i.compile_log"
	exit 1
    fi
done

if [ "x${target}" = "xandroid" ];
then
	# Install parser
	PARSER_DIR=${OUTPUT_DIR}/parser
	install -d ${PARSER_DIR}
	install -m 755 ${OUTPUT_DIR}/out/lib/libasn1c.so.0         ${PARSER_DIR}/libasn1c.so
	install -m 755 ${OUTPUT_DIR}/out/lib/libosmo-asn1-rrc.so.0 ${PARSER_DIR}/libosmo-asn1-rrc.so
	install -m 755 ${OUTPUT_DIR}/out/lib/libosmocore.so.5      ${PARSER_DIR}/libosmocore.so
	install -m 755 ${OUTPUT_DIR}/out/lib/libosmogsm.so.5       ${PARSER_DIR}/libosmogsm.so
	install -m 755 ${OUTPUT_DIR}/out/metagsm/diag_import       ${PARSER_DIR}/libdiag_import.so
	install -m 755 ${OUTPUT_DIR}/out/metagsm/libcompat.so      ${PARSER_DIR}/libcompat.so

	install -m 644 ${OUTPUT_DIR}/out/metagsm/sm_2g.sql    ${PARSER_DIR}/sm_2g.sql
	install -m 644 ${OUTPUT_DIR}/out/metagsm/sm_3g.sql    ${PARSER_DIR}/sm_3g.sql
	install -m 644 ${OUTPUT_DIR}/out/metagsm/mcc.sql      ${PARSER_DIR}/mcc.sql
	install -m 644 ${OUTPUT_DIR}/out/metagsm/mnc.sql      ${PARSER_DIR}/mnc.sql
	install -m 644 ${OUTPUT_DIR}/out/metagsm/hlr_info.sql ${PARSER_DIR}/hlr_info.sql
	install -m 644 ${OUTPUT_DIR}/out/metagsm/sm.sql       ${PARSER_DIR}/sm.sql

	install -m 644 ${BASE_DIR}/metagsm/cell_info.sql ${PARSER_DIR}/cell_info.sql
	install -m 644 ${BASE_DIR}/metagsm/si.sql        ${PARSER_DIR}/si.sql
	install -m 644 ${BASE_DIR}/metagsm/sms.sql       ${PARSER_DIR}/sms.sql
	
	# Put the smime crt into the library directory since it needs to be a physical 
	# file on the Android system so that it can be accessed from the openssl binary. 
	# Other parts of the App like assets are not stored as read files on the Android 
	# system and therefore can only be used from the Android java code but not from 
	# native binaries.
	install -m 755 $BASE_DIR/smime.crt                         ${PARSER_DIR}/libsmime_crt.so

	# Really dirty hack: The Android build system and package installer require 
	# all files in the native library dir to have a filename like libXXX.so. If
	# the file extension ends with .so.5, it will not be copied to the APK file. 
	# So the following line of perl patches all references so that the libraries
	# are found with a .so extension instead of .so.[digit]
	perl -i -pe 's/libasn1c\.so\.0/libasn1c.so\0\0/gs;s/libosmo-asn1-rrc\.so\.0/libosmo-asn1-rrc.so\0\0/gs;s/libosmocore\.so\.5/libosmocore.so\0\0/gs;s/libosmogsm\.so\.5/libosmogsm.so\0\0/gs' ${PARSER_DIR}/*.so
fi

ln -sf ${BUILD_DIR} ../${LATEST}
echo DONE
