# -*- coding: utf-8 -*-
"""
Created on Tue Jun 29 16:35:39 2021

@author: gennady
"""

import sys
import numpy as np
import pandas as pd
from sklearn import manifold

#print(f"Arguments count: {len(sys.argv)}")
#for i, arg in enumerate(sys.argv):
#    print(f"Argument {i:>6}: {arg}")
#print(sys.argv[0])
#print(sys.argv[1])
        
fname='distances4tsne-1'
if len(sys.argv)>1 and sys.argv[1] is not None:
    fname=sys.argv[1]
A = np.genfromtxt(fname+'.csv',delimiter=',',dtype=None)

perplexities = [30] #[5, 10, 20, 30, 40, 50, 75, 100]

for i,perplexity in enumerate(perplexities):
    model = manifold.TSNE(metric="precomputed", perplexity=perplexity)
    Y = model.fit_transform(A) 
    tsne_df = pd.DataFrame({'X':Y[:,0],'Y':Y[:,1]})
    tsne_df.to_csv(fname+'_out_p'+str(perplexity)+'.csv')

