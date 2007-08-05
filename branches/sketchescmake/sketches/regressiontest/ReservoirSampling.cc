// Sketches Library
//
// Copyright (C) 2005 Marios Hadjieleftheriou
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
//  Email:
//    mhadji@gmail.com

#include <Sketches.h>

int main(int argc, char** argv)
{
	if (argc != 5)
	{
		std::cerr
			<< std::endl
			<< "Usage: ReservoirSampling " 
			<< "stream_length domain_size skew sample_size.\n"
			<< "Parameters:\n"
			<< "  stream_length: Total number of insertions.\n"
			<< "  domain_size:   Total number of distinct elements.\n"
			<< "  skew:          The skew parameter of the Zipf distribution.\n"
			<< "  sample_size:   The number of sample entries.\n"
			<< std::endl;
		return -1;
	}

	size_t N = atol(argv[1]);
	size_t domainSize = atol(argv[2]);
	double skew  = atof(argv[3]);
	size_t sampleSize = atol(argv[4]);

	Tools::Random rr;
	Sketches::ReservoirSampling<uint32_t> rsL(sampleSize, rr);
	Sketches::ReservoirSampling<std::string> rsS(sampleSize, rr);
	std::map<uint64_t, uint64_t> exact;
	std::map<uint64_t, uint64_t>::iterator itEx;

	Tools::Random r;
	Tools::PRGZipf zipf(0L, domainSize, skew, &r);

	std::cerr << std::setprecision(2) << std::fixed;
	std::cout << std::setprecision(2) << std::fixed;

	double totalErrorL = 0.0;
	double totalErrorS = 0.0;
	size_t totalAnswers = 0;

	try
	{
		std::cerr << "Testing insertions." << std::endl;

		for (size_t i = 1; i <= N; i++)
		{
			uint32_t l = zipf.nextLong();
			std::ostringstream ss;
			ss << l << std::flush;

			rsL.insert(l);
			rsS.insert(ss.str());

			itEx = exact.find(l);
			if (itEx != exact.end())
				(*itEx).second++;
			else
				exact[l] = 1;

			if (i % 100 == 0)
			{
				// find top 20 most frequent.
				std::multimap<uint64_t, uint64_t> frequent;
				for (itEx = exact.begin(); itEx != exact.end(); itEx++)
					frequent.insert(
						std::pair<uint64_t, uint64_t>(
							(*itEx).second,
							(*itEx).first)
					);

				std::multimap<uint64_t, uint64_t>::iterator it = frequent.end(); it--;

				size_t top = 20;
				while (top > 0)
				{
					uint64_t fL = rsL.getFrequency((*it).second);
					std::ostringstream ss2;
					ss2 << (*it).second << std::flush;
					uint64_t fS = rsS.getFrequency(ss2.str());

					double eL =
						std::abs(static_cast<double>(fL) - static_cast<double>((*it).first)) /
						static_cast<double>((*it).first);

					double eS =
						std::abs(static_cast<double>(fS) - static_cast<double>((*it).first)) /
						static_cast<double>((*it).first);

					totalErrorL += eL;
					totalErrorS += eS;
					totalAnswers++;

					top--;
					it--;
				}

				std::cerr << "Insertions: " << i << std::endl;
			}
		}

		std::cerr
			<< "Average relative error: "
			<< totalErrorL / totalAnswers << " | " << totalErrorS / totalAnswers
			<< std::endl;

		std::cerr << "Testing getData operations." << std::endl;

		size_t len, len2;
		byte *data, *data2;

		rsL.getData(&data, len);
		Sketches::ReservoirSampling<uint32_t> rsC(data, rr);
		rsC.getData(&data2, len2);

		if (len != len2 || memcmp(data, data2, len) != 0)
			throw Tools::IllegalStateException(
				"Error: The getData operation does not work correctly!"
			);

		delete[] data;
		delete[] data2;

		rsS.getData(&data, len);
		Sketches::ReservoirSampling<std::string> rsC2(data, rr);
		rsC2.getData(&data2, len2);

		if (len != len2 || memcmp(data, data2, len) != 0)
			throw Tools::IllegalStateException(
				"Error: The getData operation does not work correctly!"
			);

		delete[] data;
		delete[] data2;

		std::cerr << "  O.k." << std::endl;
		std::cerr << "Testing copy constructor." << std::endl;

		Sketches::ReservoirSampling<std::string> rsC3(rsS);
		rsS.getData(&data, len);
		rsC3.getData(&data2, len2);

		if (len != len2 || memcmp(data, data2, len) != 0)
			throw Tools::IllegalStateException(
				"Error: The copy constructor does not work correctly!"
			);

		delete[] data;
		delete[] data2;

		std::cerr << "  O.k." << std::endl;
	}
	catch (Tools::Exception& e)
	{
		std::cerr << e.what() << std::endl;
		return -1;
	}

	return 0;
}

