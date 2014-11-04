#!/usr/bin/perl

use strict;
use warnings;

sub read_file
{
	my $infile = shift;	
	my $handle;

	open($handle, $infile) or die "$infile: $!";

	while (<$handle>)
	{
		chomp;
		if (/^\.read\s+(.*)$/)
		{
			read_file($1);
		} else
		{
			print($_ . "\n");
		}
	}
	close $handle;
}

print "--  AUTOMATICALLY GENERATED - DO NOT EDIT!\n\n";
read_file($ARGV[0]);
